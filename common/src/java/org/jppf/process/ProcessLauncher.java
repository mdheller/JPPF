/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.process;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.node.idle.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * <p>This class is intended as a controller for a subprocess, to enable stopping and restarting it when requested.
 * <p>It performs the following operations:
 * <ul>
 * <li>open a server socket the driver will listen to (port number is dynamically attributed)</li>
 * <li>Start the subprocess, sending the server socket port number as an argument</li>
 * <li>Wait for the subprocess to exit</li>
 * <li>If the subprocess exit code is equal to 2, the subprocess is restarted, otherwise this process exits as well</li>
 * </ul>
 * @author Laurent Cohen
 */
public class ProcessLauncher extends AbstractProcessLauncher implements IdleStateListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProcessLauncher.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The fully qualified name of the main class of the subprocess to launch.
   */
  private final String mainClass;
  /**
   * Determines whether the process was stopped because the system went into "busy state".
   */
  private final AtomicBoolean stoppedOnBusyState = new AtomicBoolean(false);
  /**
   * Determines whether the system is in "idle state".
   */
  private final AtomicBoolean idle = new AtomicBoolean(false);
  /**
   * Whether idle mode can be used on the process.
   */
  private final boolean idleModeSupported;
  /**
   * Specifies whether the subprocess is launched only when the system is idle.
   */
  private boolean idleMode = false;
  /**
   * Detects system idle state changes.
   */
  private IdleDetector idleDetector = null;

  /**
   * Initialize this process launcher.
   * @param mainClass the fully qualified name of the main class of the sub process to launch.
   * @param idleModeSupported whether idle mode can be used on the process.
   */
  public ProcessLauncher(final String mainClass, final boolean idleModeSupported) {
    if (mainClass == null) throw new IllegalArgumentException("the main class name cannot be null");
    this.mainClass = mainClass;
    this.idleModeSupported = idleModeSupported;
    int idx = mainClass.lastIndexOf('.');
    this.name = (idx < 0) ? mainClass : mainClass.substring(idx);
  }

  /**
   * Start the socket listener and the subprocess.
   */
  @Override
  public void run() {
    if (idleModeSupported) {
      TypedProperties config = JPPFConfiguration.getProperties();
      idleMode = config.getBoolean("jppf.idle.mode.enabled", false);
    }
    boolean end = false;
    try {
      createShutdownHook();
      startSocketListener();
      if (idleMode) {
        idleDetector = new IdleDetector(this);
        System.out.println("Node running in \"Idle Host\" mode");
        idleDetector.run();
      }
      while (!end) {
        if (idleMode) while (!idle.get()) goToSleep();
        startProcess();
        int n = process.waitFor();
        end = onProcessExit(n);
        if (process != null) process.destroy();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.exit(0);
  }

  /**
   * Start the sub-process.
   * @throws Exception if any error occurs.
   */
  public void startProcess() throws Exception {
    stoppedOnBusyState.set(false);
    process = buildProcess();
    if (debugEnabled) log.debug("started driver process [" + process + ']');
  }

  /**
   * Start the JPPF driver subprocess.
   * @return A reference to the {@link Process} object representing the JPPF driver subprocess.
   * @throws Exception if the process failed to start.
   */
  private Process buildProcess() throws Exception {
    TypedProperties config = JPPFConfiguration.getProperties();
    String s = config.getString("jppf.jvm.options");
    Pair<List<String>, List<String>> parsed = parseJvmOptions(s);
    List<String> jvmOptions = parsed.first();
    List<String> cpElements = parsed.second();
    cpElements.add(0, System.getProperty("java.class.path"));
    List<String> command = new ArrayList<>();
    command.add(System.getProperty("java.home")+"/bin/java");
    command.add("-cp");
    StringBuilder sb = new StringBuilder();
    String sep = System.getProperty("path.separator");
    for (int i=0; i<cpElements.size(); i++) {
      if (i > 0) sb.append(sep);
      sb.append(cpElements.get(i));
    }
    command.add(sb.toString());
    for (String opt: jvmOptions) command.add(opt);
    s = System.getProperty(JPPFConfiguration.CONFIG_PROPERTY);
    if (s != null) command.add("-D" + JPPFConfiguration.CONFIG_PROPERTY + '=' + s);
    s = System.getProperty(JPPFConfiguration.CONFIG_PLUGIN_PROPERTY);
    if (s != null) command.add("-D" + JPPFConfiguration.CONFIG_PLUGIN_PROPERTY + '=' + s);
    command.add("-Dlog4j.configuration=" + System.getProperty("log4j.configuration"));
    command.add(mainClass);
    command.add(Integer.toString(processPort));
    if (debugEnabled) log.debug("process command:\n" + command);
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectError(Redirect.INHERIT);
    builder.redirectOutput(Redirect.INHERIT);
    return builder.start();
  }

  /**
   * Called when the subprocess has exited with exit value n.
   * This allows for printing the residual output (both standard and error) to this pJVM's console and log file,
   * in order to get additional information if a problem occurred.
   * @param n the exit value of the subprocess.
   * @return true if this launcher is to be terminated, false if it should re-launch the subprocess.
   */
  protected boolean onProcessExit(final int n) {
    String s = getOutput(process, "std").trim();
    if (s.length() > 0) {
      System.out.println("\nstandard output:\n" + s);
      log.info("standard output:\n" + s);
    }
    s = getOutput(process, "err").trim();
    if (s.length() > 0) {
      System.out.println("\nerror output:\n" + s);
      log.info("error output:\n" + s);
    }
    return (n != 2) && !stoppedOnBusyState.get();
  }

  /**
   * Get the output of the driver process.
   * @param process the process to get the standard or error output from.
   * @param streamType determines whether to obtain the standard or error output.
   * @return the output as a string.
   */
  public String getOutput(final Process process, final String streamType) {
    StringBuilder sb = new StringBuilder();
    try {
      InputStream is = "std".equals(streamType) ? process.getInputStream() : process.getErrorStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      try {
        String s = "";
        while (s != null) {
          s = reader.readLine();
          if (s != null) sb.append(s).append('\n');
        }
      } finally {
        reader.close();
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    return sb.toString();
  }

  @Override
  public void idleStateChanged(final IdleStateEvent event) {
    IdleState state = event.getState();
    if (IdleState.BUSY.equals(state)) {
      if (idleMode && (process != null)) {
        idle.set(false);
        stoppedOnBusyState.set(true);
        boolean b = JPPFConfiguration.getProperties().getBoolean("jppf.idle.interruptIfRunning", true);
        int action = b ? ProcessCommands.SHUTDOWN_INTERRUPT : ProcessCommands.SHUTDOWN_NO_INTERRUPT;
        if (debugEnabled) log.debug("sending command {}", ProcessCommands.getCommandName(action));
        //process.destroy();
        sendActionCommand(action);
      }
    } else {
      idle.set(true);
      wakeUp();
    }
  }
}
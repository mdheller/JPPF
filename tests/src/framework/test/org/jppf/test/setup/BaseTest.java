/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package test.org.jppf.test.setup;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jppf.location.FileLocation;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class BaseTest {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger("TEST");
  /** */
  private static String name;
  /** */
  private static PrintStream stdOut, stdErr;
  /** */
  private static FileFilter logFileFilter = new FileFilter() {
    @Override
    public boolean accept(final File path) {
      if (path.isDirectory()) return false;
      String s = path.getName();
      return (s != null) && s.endsWith(".log");
    }
  };
  /**
   * Used to format timestamps in the std and err outputs.
   */
  private static final SimpleDateFormat SDF = new SimpleDateFormat("hh:mm:ss.SSS");
  /** */
  @ClassRule
  public static TestWatcher classWatcher = new BaseTestClassWatcher();
  /** */
  @Rule
  public TestWatcher instanceWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      print("***** start of method %s() *****", description.getMethodName());
    }
  };

  /**
   * Zip all log files into the file {@code logs/<className>.zip}.
   * @param className the name of the class for which to zip the logs.
   */
  private static void zipLogs(final String className) {
    FileLocation src = new FileLocation("jppf-client.log");
    try {
      src.copyTo(new FileLocation(new File("client.log")));
    } catch (Exception e) {
      e.printStackTrace();
    }
    File dir = new File(System.getProperty("user.dir"));
    File[] logFiles = dir.listFiles(logFileFilter);
    if ((logFiles == null) || (logFiles.length <= 0)) return;
    File logDir = new File(dir, "logs/");
    if (!logDir.exists()) logDir.mkdirs();
    File outZip = new File(logDir, className + ".zip");
    String[] logPaths = new String[logFiles.length];
    for (int i=0; i<logFiles.length; i++) logPaths[i] = logFiles[i].getPath();
    ZipUtils.zipFile(outZip.getPath(), logPaths);
  }

  /**
   * Print a formatted message to the shell output only.
   * @param format the format.
   * @param params the parameter values.
   */
  public static void printOut(final String format, final Object...params) {
    print(true, false, format, params);
  }

  /**
   * Print a formatted message to the shell output and to the log file.
   * @param format the format.
   * @param params the parameter values.
   */
  public static void print(final String format, final Object...params) {
    print(false, true, format, params);
  }

  /**
   * Print a formatted message to the shell output and to the log file.
   * @param decorate whether to add decorations around the message.
   * @param format the format.
   * @param params the parameter values.
   */
  public static void print(final boolean decorate, final String format, final Object...params) {
    print(false, decorate, format, params);
  }

  /**
   * Print a formatted message to the shell output and to the log file.
   * @param systemOutOnly whether to print to {@code System.out} only.
   * @param decorate whether to add decorations around the message.
   * @param format the format.
   * @param params the parameter values.
   */
  public static void print(final boolean systemOutOnly, final boolean decorate, final String format, final Object...params) {
    String message = String.format(format, params);
    System.out.printf("[%s] %s%n", getFormattedTimestamp(), message);
    if (!systemOutOnly) {
      String s = "";
      if (decorate) {
        StringBuilder sb = new StringBuilder("*****");
        for (int i=0; i<message.length()-10; i++) sb.append('-');
        sb.append("*****");
        s = sb.toString();
      }
      if (decorate) log.info(s);
      log.info(message);
      if (decorate) log.info(s);
    }
  }

  /**
   * Get the current timestamp as a formatted string.
   * @return the timestamp formatted according to {@link #SDF}.
   */
  public static String getFormattedTimestamp() {
    return getFormattedTimestamp(new Date());
  }

  /**
   * Get the current timestamp as a formatted string.
   * @param timestamp the timestamp to format.
   * @return the timestamp formatted according to {@link #SDF}.
   */
  public static String getFormattedTimestamp(final long timestamp) {
    return getFormattedTimestamp(new Date(timestamp));
  }

  /**
   * Get the current timestamp as a formatted string.
   * @param date the date to format.
   * @return the timestamp formatted according to {@link #SDF}.
   */
  public static String getFormattedTimestamp(final Date date) {
    synchronized(SDF) {
      return SDF.format(date);
    }
  }

  /**
   * Execute a script on the specified driver.
   * @param jmx JMX wrapper for the driver.
   * @param script the script to execute.
   * @return whatever the script returns.
   * @throws Exception if any error occurs.
   */
  public static Object executeScriptOnServer(final JMXDriverConnectionWrapper jmx, final String script) throws Exception {
    return jmx.invoke("org.jppf:name=debug,type=driver", "executeScript", new Object[] { "javascript", script}, new String[] {"java.lang.String", "java.lang.String"});
  }

  /** */
  public static class BaseTestClassWatcher extends TestWatcher {
    @Override
    protected void starting(final Description description) {
      // delete the drivers and nodes log files if they exist
      org.apache.log4j.LogManager.resetConfiguration();
      File dir = new File(System.getProperty("user.dir"));
      File[] logFiles = dir.listFiles(logFileFilter);
      if (logFiles != null) {
        for (File file: logFiles) {
          if (file.exists()) {
            if (!file.delete()) System.err.printf("[%s] Could not delete %s%n", getFormattedTimestamp(), file);
          }
        }
      }
      File slavesDir = new File(dir, "slave_nodes");
      if (slavesDir.exists()) {
        if (!FileUtils.deletePath(slavesDir)) print("Could not delete '%s'", slavesDir);
      }
      org.apache.log4j.PropertyConfigurator.configure("classes/tests/config/log4j-client.properties");
      // redirect System.out and System.err to files
      stdOut = System.out;
      stdErr = System.err;
      try {
        System.setOut(new PrintStream("std_out.log"));
        System.setErr(new PrintStream("std_err.log"));
      } catch (Exception e) {
        print("Error redirecting std_out or std_err: %s", ExceptionUtils.getStackTrace(e));
      }
      print("***** start of class %s *****", description.getClassName());
    }

    @Override
    protected void finished(final Description description) {
      print("***** finished class %s *****", description.getClassName());
      try {
        // redirect System.out and System.err back to their original destination
        if ((stdOut != null) && (stdOut != System.out)) {
          PrintStream tmp = System.out;
          System.setOut(stdOut);
          tmp.close();
        }
        if ((stdErr != null) && (stdErr != System.err)) {
          PrintStream tmp = System.err;
          System.setErr(stdErr);
          tmp.close();
        }
      } catch (Exception e) {
        print("Error restoring std_out or std_err: %s", ExceptionUtils.getStackTrace(e));
      }
      File dir = new File(System.getProperty("user.dir"));
      File slavesDir = new File(dir, "slave_nodes");
      if (slavesDir.exists() && slavesDir.isDirectory()) {
        File[] subdirs = slavesDir.listFiles(new FileFilter() {
          @Override
          public boolean accept(final File file) {
            return file.isDirectory();
          }
        });
        if (subdirs != null) {
          for (File subdir: subdirs) {
            File[] logFiles = subdir.listFiles(logFileFilter);
            if (logFiles != null) {
              for (File logFile: logFiles) {
                String path = subdir.getName() + "_" + logFile.getName();
                try {
                  StreamUtils.copyFile(logFile, new File(dir, path));
                } catch (IOException e) {
                  print("Error copying '%s' to '%s': %s", ExceptionUtils.getStackTrace(e));
                }
              }
            }
          }
        }
      }
      zipLogs(description.getClassName());
    }
  }
}
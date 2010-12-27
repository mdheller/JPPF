/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.server.protocol;

import java.io.File;
import java.util.*;

import org.jppf.process.ProcessWrapper;
import org.jppf.process.event.*;
import org.jppf.utils.CollectionUtils;

/**
 * Instances of this class encapsulate the execution of an external process, program or shell script.<br>
 * This task starts and external process using command line arguments, environment variables, and a list
 * of input and/or output files to use or generated by the external process.<br>
 * This task also captures the standard and error output (i.e. equivalent to System.out and System.err) of the
 * external process.
 * @author Laurent Cohen
 */
public abstract class CommandLineTask extends JPPFTask implements ProcessWrapperEventListener
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The list of command-line arguments.
	 */
	private List<String> commandList = new ArrayList<String>();
	/**
	 * The environment variables to set.
	 */
	private Map<String, String> env = new HashMap<String, String>();
	/**
	 * The directory to start the command in.
	 */
	private String startDir = null;
	/**
	 * Content of the standard output for the process.
	 */
	private StringBuilder standardOutput = new StringBuilder();
	/**
	 * Content of the error output for the process.
	 */
	private StringBuilder errorOutput = new StringBuilder();
	/**
	 * Determines whether the process output should be captured.
	 */
	private boolean captureOutput = false;

	/**
	 * Default condtructor.
	 */
	public CommandLineTask()
	{
	}

	/**
	 * Create an instance of this class and set the parameters of the external process or script to launch.
	 * @param commands the list of command-line arguments.
	 */
	public CommandLineTask(String...commands)
	{
		this(null, null, commands);
	}

	/**
	 * Create an instance of this class and set the parameters of the external process or script to launch.
	 * @param env the environment variables to set.
	 * @param startDir the directory to start the command in.
	 * @param commands the list of command-line arguments.
	 */
	public CommandLineTask(Map<String, String> env, String startDir, String...commands)
	{
		if (commands != null)
		{
			for (String s: commands) commandList.add(s);
		}
		this.env = env;
		this.startDir = startDir;
	}

	/**
	 * Run the external process or script.
	 * @throws Exception if an error occurs.
	 */
	public void launchProcess() throws Exception
	{
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(commandList);
		if (startDir != null) builder.directory(new File(startDir));
		if (env != null)
		{
			Map<String, String> map = builder.environment();
			for (Map.Entry<String, String> e: env.entrySet()) map.put(e.getKey(), e.getValue());
		}
		ProcessWrapper wrapper = new ProcessWrapper();
		if (captureOutput) wrapper.addListener(this);
		Process p = builder.start();
		wrapper.setProcess(p);
		p.waitFor();
		if (captureOutput) wrapper.removeListener(this);
	}

	/**
	 * Determines whether the process output is captured.
	 * @return true if the output is cpatured, false otherwise.
	 */
	public boolean isCaptureOutput()
	{
		return captureOutput;
	}

	/**
	 * Specifies whether the process output is captured.
	 * @param captureOutput true if the output is cpatured, false otherwise.
	 */
	public void setCaptureOutput(boolean captureOutput)
	{
		this.captureOutput = captureOutput;
	}

	/**
	 * Get the content of the standard output for the process.
	 * @return the output as a string.
	 */
	public String getStandardOutput()
	{
		return standardOutput.toString();
	}

	/**
	 * Get the content of the error output for the process.
	 * @return the output as a string.
	 */
	public String getErrorOutput()
	{
		return errorOutput.toString();
	}

	/**
	 * Get the list of command-line arguments.
	 * @return a list of arguments as strings.
	 */
	public List<String> getCommandList()
	{
		return commandList;
	}

	/**
	 * Set the list of command-line arguments.
	 * @param commandList a list of arguments as strings.
	 */
	public void setCommandList(List<String> commandList)
	{
		this.commandList = commandList;
	}

	/**
	 * Set the list of command-line arguments.
	 * @param commands a list of arguments as strings.
	 */
	public void setCommandList(String...commands)
	{
		commandList = CollectionUtils.list(commands);
	}

	/**
	 * Get the environment variables to set.
	 * @return a map of variable names to their corresponding values.
	 */
	public Map<String, String> getEnv()
	{
		return env;
	}

	/**
	 * Get the environment variables to set.
	 * @param env a map of variable names to their corresponding values.
	 */
	public void setEnv(Map<String, String> env)
	{
		this.env = env;
	}

	/**
	 * Get the directory to start the command in.
	 * @return the start directory as a string.
	 */
	public String getStartDir()
	{
		return startDir;
	}

	/**
	 * Set the directory to start the command in.
	 * @param startDir the start directory as a string.
	 */
	public void setStartDir(String startDir)
	{
		this.startDir = startDir;
	}

	/**
	 * Notification that the process has written to its output stream.
	 * @param event encapsulates the output stream's content.
	 * @see org.jppf.process.event.ProcessWrapperEventListener#outputStreamAltered(org.jppf.process.event.ProcessWrapperEvent)
	 */
	public void outputStreamAltered(ProcessWrapperEvent event)
	{
		standardOutput.append(event.getContent()).append("\n");
	}

	/**
	 * Notification that the process has written to its error stream.
	 * @param event encapsulate the error stream's content.
	 * @see org.jppf.process.event.ProcessWrapperEventListener#errorStreamAltered(org.jppf.process.event.ProcessWrapperEvent)
	 */
	public void errorStreamAltered(ProcessWrapperEvent event)
	{
		errorOutput.append(event.getContent()).append("\n");
	}
}

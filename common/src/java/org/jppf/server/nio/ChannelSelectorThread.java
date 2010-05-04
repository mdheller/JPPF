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

package org.jppf.server.nio;

import org.jppf.utils.ThreadSynchronization;

/**
 * 
 * @author Laurent Cohen
 */
public class ChannelSelectorThread extends ThreadSynchronization implements Runnable
{
	/**
	 * The channel selector associated with this thread.
	 */
	private ChannelSelector selector = null;
	/**
	 * The nio server that own this thread.
	 */
	private NioServer<?, ?> server = null;

	/**
	 * Initialize this threadwith the specified name, selector and NIO server.
	 * @param selector the channel selector associated with this thread.
	 * @param server the nio server that own this thread.
	 */
	public ChannelSelectorThread(ChannelSelector selector, NioServer<?, ?> server)
	{
		this.selector = selector;
		this.server = server;
	}

	/**
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		while (!isStopped())
		{
			if (selector.select(10)) server.getTransitionManager().submitTransition(selector.getChannel());
		}
	}
}

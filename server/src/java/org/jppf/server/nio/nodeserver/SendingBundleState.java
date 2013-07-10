/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;

import java.net.ConnectException;

import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * This class represents the state of waiting for some action.
 * @author Laurent Cohen
 */
class SendingBundleState extends NodeServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SendingBundleState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public SendingBundleState(final NodeNioServer server)
  {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public NodeTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    //if (debugEnabled) log.debug("exec() for " + getRemoteHost(channel));
    if (channel.isReadable() && !channel.isLocal()) throw new ConnectException("node " + channel + " has been disconnected");

    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    if (context.getMessage() == null)
    {
      ServerTaskBundleNode nodeBundle = context.getBundle();
      JPPFTaskBundle bundle = (nodeBundle == null) ? null : nodeBundle.getJob();
      if (bundle != null)
      {
        if (debugEnabled) log.debug("got bundle " + nodeBundle + " from the queue for " + channel);
        // to avoid cycles in peer-to-peer routing of jobs.
        if (bundle.getUuidPath().contains(context.getUuid()))
        {
          if (debugEnabled) log.debug("cycle detected in peer-to-peer bundle routing: " + bundle.getUuidPath());
          context.setBundle(null);
          nodeBundle.resubmit();
          return context.isPeer() ? TO_IDLE_PEER : TO_IDLE;
        }
        nodeBundle.getJob().setExecutionStartTime(System.nanoTime());
        context.serializeBundle(channel);
      }
      else
      {
        if (debugEnabled) log.debug("null bundle for node " + channel);
        return context.isPeer() ? TO_IDLE_PEER : TO_IDLE;
      }
    }
    if (context.writeMessage(channel))
    {
      if (debugEnabled) log.debug("sent entire bundle " + context.getBundle() + " to node " + channel);
      context.setMessage(null);
      //JPPFDriver.getInstance().getJobManager().jobDispatched(context.getBundle(), channel);
      return TO_WAITING_RESULTS;
    }
    if (traceEnabled) log.trace("part yet to send to node " + channel);
    return TO_SENDING_BUNDLE;
  }
}
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

package org.jppf.server.node.remote;

import static org.jppf.server.protocol.BundleParameter.NODE_EXCEPTION_PARAM;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.io.*;
import org.jppf.node.protocol.Task;
import org.jppf.server.node.*;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.ObjectSerializer;
import org.slf4j.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public class RemoteNodeIO extends AbstractNodeIO
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RemoteNodeIO.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The underlying socket wrapper.
   */
  private SocketWrapper socketWrapper = null;

  /**
   * Initialize this TaskIO with the specified node.
   * @param node - the node who owns this TaskIO.
   */
  public RemoteNodeIO(final JPPFNode node)
  {
    super(node);
    this.socketWrapper = ((RemoteNodeConnection) node.getNodeConnection()).getChannel();
  }

  @Override
  protected Object[] deserializeObjects() throws Exception
  {
    ObjectSerializer ser = node.getHelper().getSerializer();
    if (debugEnabled) log.debug("waiting for next request. Serializer = " + ser + " (class loader = " + ser.getClass().getClassLoader() + ")");
    JPPFTaskBundle bundle = (JPPFTaskBundle) IOHelper.unwrappedData(socketWrapper, node.getHelper().getSerializer());
    if (debugEnabled) log.debug("got bundle " + bundle);
    node.getExecutionManager().setBundle(bundle);
    return deserializeObjects(bundle);
  }

  @Override
  protected Object[] deserializeObjects(final JPPFTaskBundle bundle) throws Exception
  {
    int count = bundle.getTaskCount();
    List<Object> list = new ArrayList<Object>(count + 2);
    list.add(bundle);
    try
    {
      initializePerformanceData(bundle);
      if (debugEnabled) log.debug("bundle task count = " + count + ", state = " + bundle.getState());
      if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
      {
        JPPFContainer cont = node.getContainer(bundle.getUuidPath().getList());
        cont.getClassLoader().setRequestUuid(bundle.getRequestUuid());
        node.getLifeCycleEventHandler().fireJobHeaderLoaded(bundle, cont.getClassLoader());
        cont.deserializeObjects(list, 1+count, node.getExecutionManager().getExecutor());
      }
      else
      {
        // skip null data provider
        socketWrapper.receiveBytes(0);
      }
      if (debugEnabled) log.debug("got all data");
    }
    catch(Throwable t)
    {
      log.error("Exception occurred while deserializing the tasks", t);
      bundle.setTaskCount(0);
      bundle.setParameter(NODE_EXCEPTION_PARAM, t);
    }
    return list.toArray(new Object[list.size()]);
  }

  /**
   * Performs the actions required if reloading the classes is necessary.
   * @throws Exception if any error occurs.
   * @see org.jppf.server.node.AbstractNodeIO#handleReload()
   */
  @Override
  protected void handleReload() throws Exception
  {
    node.setClassLoader(null);
    node.initHelper();
    socketWrapper.setSerializer(node.getHelper().getSerializer());
  }

  /**
   * Write the execution results to the socket stream.
   * @param bundle the task wrapper to send along.
   * @param tasks the list of tasks with their result field updated.
   * @throws Exception if an error occurs while writing to the socket stream.
   * @see org.jppf.server.node.NodeIO#writeResults(org.jppf.server.protocol.JPPFTaskBundle, java.util.List)
   */
  @Override
  public void writeResults(final JPPFTaskBundle bundle, final List<Task> tasks) throws Exception
  {
    if (debugEnabled) log.debug("writing results for " + bundle);
    ExecutorService executor = node.getExecutionManager().getExecutor();
    finalizePerformanceData(bundle);
    List<Future<DataLocation>> futureList = new ArrayList<Future<DataLocation>>(tasks.size() + 1);
    futureList.add(executor.submit(new ObjectSerializationTask(bundle)));
    for (Task task : tasks) futureList.add(executor.submit(new ObjectSerializationTask(task)));
    OutputDestination dest = new SocketWrapperOutputDestination(socketWrapper);
    for (Future<DataLocation> f: futureList)
    {
      DataLocation dl = f.get();
      if (traceEnabled) log.trace("writing object size = " + dl.getSize());
      IOHelper.writeData(dl, dest);
    }
    socketWrapper.flush();
    if (debugEnabled) log.debug("wrote full results");
  }
}
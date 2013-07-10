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

package org.jppf.server.node;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.*;
import org.jppf.node.ThreadManager.UsedClassLoader;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFScheduleHandler;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @author Paul Woodward
 */
public class NodeExecutionManagerImpl
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeExecutionManagerImpl.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node that uses this execution manager.
   */
  private NodeInternal node = null;
  /**
   * Timer managing the tasks timeout.
   */
  private final JPPFScheduleHandler timeoutHandler = new JPPFScheduleHandler("Task Timeout Timer");
  /**
   * The bundle whose tasks are currently being executed.
   */
  private JPPFTaskBundle bundle = null;
  /**
   * The list of tasks to execute.
   */
  private List<? extends Task> taskList = null;
  /**
   * The uuid path of the current bundle.
   */
  private List<String> uuidList = null;
  /**
   * Holds a the tasks submitted tot he executor.
   */
  private List<NodeTaskWrapper> taskWrapperList = null;
  /**
   * List of listeners to task execution events.
   */
  private final List<TaskExecutionListener> taskExecutionListeners = new CopyOnWriteArrayList<TaskExecutionListener>();
  /**
   * Determines whether the number of threads or their priority has changed.
   */
  private final AtomicBoolean configChanged = new AtomicBoolean(true);
  /**
   * Set if the node must reconnect to the driver.
   */
  private AtomicReference<JPPFNodeReconnectionNotification> reconnectionNotification = new AtomicReference<JPPFNodeReconnectionNotification>(null);
  /**
   * The thread manager that is used for execution.
   */
  private final ThreadManager threadManager;
  /**
   * Determines whether the current job has been cancelled.
   */
  private AtomicBoolean jobCancelled = new AtomicBoolean(false);
  /**
   * The class loader used to load the tasks and the classes they need from the client.
   */
  private UsedClassLoader usedClassLoader = null;
  /**
   * The data provider for the current job.
   */
  private DataProvider dataProvider = null;
  /**
   * The total accumulated elapsed time of the tasks in the current bundle.
   */
  private final AtomicLong accumulatedElapsed = new AtomicLong(0L);

  /**
   * Initialize this execution manager with the specified node.
   * @param node the node that uses this execution manager.
   */
  public NodeExecutionManagerImpl(final AbstractNode node) {
    this(node, "processing.threads");
  }

  /**
   * Initialize this execution manager with the specified node.
   * @param node the node that uses this execution manager.
   * @param nbThreadsProperty the name of the property which configures the number of threads.
   */
  public NodeExecutionManagerImpl(final NodeInternal node, final String nbThreadsProperty) {
    if (node == null) throw new IllegalArgumentException("node is null");
    this.node = node;
    TypedProperties config = JPPFConfiguration.getProperties();
    int poolSize = config.getInt(nbThreadsProperty, Runtime.getRuntime().availableProcessors());
    if (poolSize <= 0) {
      poolSize = Runtime.getRuntime().availableProcessors();
      config.setProperty(nbThreadsProperty, Integer.toString(poolSize));
    }
    log.info("running " + poolSize + " processing thread" + (poolSize > 1 ? "s" : ""));
    threadManager = createThreadManager(config, poolSize);
  }

  /**
   * Create the thread manager instance. Default is {@link ThreadManagerThreadPool}.
   * @param config The JPPF configuration properties.
   * @param poolSize the initial pool size.
   * @return an instance of {@link ThreadManager}.
   */
  private static ThreadManager createThreadManager(final TypedProperties config, final int poolSize)
  {
    ThreadManager result = null;
    String s = config.getString("jppf.thread.manager.class", "default");
    if (!"default".equalsIgnoreCase(s) && !"org.jppf.server.node.ThreadManagerThreadPool".equals(s) && s != null) {
      try {
        Class clazz = Class.forName(s);
        Object instance = ReflectionHelper.invokeConstructor(clazz, new Class[]{Integer.TYPE}, poolSize);
        if (instance instanceof ThreadManager) {
          result = (ThreadManager) instance;
          log.info("Using custom thread manager: " + s);
        }
      } catch(Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    if (result == null) {
      log.info("Using default thread manager");
      return new ThreadManagerThreadPool(poolSize);
    }
    config.setProperty("processing.threads", Integer.toString(result.getPoolSize()));
    log.info("Node running " + poolSize + " processing thread" + (poolSize > 1 ? "s" : ""));
    boolean cpuTimeEnabled = result.isCpuTimeEnabled();
    config.setProperty("cpuTimeSupported", Boolean.toString(cpuTimeEnabled));
    log.info("Thread CPU time measurement is " + (cpuTimeEnabled ? "" : "not ") + "supported");
    return result;
  }

  /**
   * Execute the specified tasks of the specified tasks bundle.
   * @param bundle the bundle to which the tasks are associated.
   * @param taskList the list of tasks to execute.
   * @throws Exception if the execution failed.
   */
  public void execute(final JPPFTaskBundle bundle, final List<? extends Task> taskList) throws Exception {
    if ((taskList == null) || taskList.isEmpty()) return;
    if (debugEnabled) log.debug("executing " + taskList.size() + " tasks");
    try {
      setup(bundle, taskList);
      if (!isJobCancelled()) {
        ExecutorCompletionService<NodeTaskWrapper> ecs = new ExecutorCompletionService<NodeTaskWrapper>(getExecutor());
        for (Task task : taskList) {
          NodeTaskWrapper taskWrapper = new NodeTaskWrapper(task, usedClassLoader.getClassLoader(), timeoutHandler);
          taskWrapperList.add(taskWrapper);
          Future<NodeTaskWrapper> f =  ecs.submit(taskWrapper, taskWrapper);
        }
        for (int i=0; i<taskList.size(); i++) {
          try {
            NodeTaskWrapper taskWrapper = ecs.take().get();
            JPPFNodeReconnectionNotification notif = taskWrapper.getReconnectionNotification();
            if (notif != null) {
              cancelAllTasks(true, false);
              throw notif;
            }
            taskEnded(taskWrapper);
          } catch (final Exception e) {
            log.debug("Exception when executing task", e);
          }
        }
      }
    } finally {
      cleanup();
    }
  }

  /**
   * Cancel all executing or pending tasks.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   */
  public void cancelAllTasks(final boolean callOnCancel, final boolean requeue) {
    if (debugEnabled) log.debug("cancelling all tasks with: callOnCancel=" + callOnCancel + ", requeue=" + requeue);
    if (requeue && (bundle != null)) {
      synchronized(bundle) {
        bundle.setParameter(BundleParameter.JOB_REQUEUE, true);
        bundle.getSLA().setSuspended(true);
      }
    }
    if (taskWrapperList != null) {
      for (NodeTaskWrapper ntw: taskWrapperList) cancelTask(ntw, callOnCancel);
    }
  }

  /**
   * Cancel the execution of the tasks with the specified id.
   * @param taskWrapper the index of the task to cancel.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   */
  private void cancelTask(final NodeTaskWrapper taskWrapper, final boolean callOnCancel) {
    if (debugEnabled) log.debug("cancelling task = " + taskWrapper);
    Future<?> future = taskWrapper.getFuture();
    if (!future.isDone()) {
      if (debugEnabled) log.debug("calling future.cancel(true) for task = " + taskWrapper);
      if (taskWrapper != null) taskWrapper.cancel(callOnCancel);
      future.cancel(true);
      taskWrapper.cancelTimeoutAction();
    }
  }

  /**
   * Shutdown this execution manager.
   */
  public void shutdown() {
    getExecutor().shutdownNow();
    timeoutHandler.clear(true);
  }

  /**
   * Prepare this execution manager for executing the tasks of a bundle.
   * @param bundle the bundle whose tasks are to be executed.
   * @param taskList the list of tasks to execute.
   */
  @SuppressWarnings("unchecked")
  private void setup(final JPPFTaskBundle bundle, final List<? extends Task> taskList) {
    this.bundle = bundle;
    this.taskList = taskList;
    this.taskWrapperList = new ArrayList<NodeTaskWrapper>(taskList.size());
    this.dataProvider = taskList.get(0).getDataProvider();
    this.uuidList = bundle.getUuidPath().getList();
    try {
      AbstractJPPFClassLoader taskClassLoader = (AbstractJPPFClassLoader) ((node instanceof ClassLoaderProvider) ? ((ClassLoaderProvider)node).getClassLoader(uuidList) : null);
      usedClassLoader = threadManager.useClassLoader(taskClassLoader);
    } catch (Exception e) {
      String msg = ExceptionUtils.getMessage(e) + " - class loader lookup failed for uuidPath=" + uuidList;
      if (debugEnabled) log.debug(msg, e);
      else log.warn(msg);
    }
    accumulatedElapsed.set(0L);
    node.getLifeCycleEventHandler().fireJobStarting(bundle, (AbstractJPPFClassLoader) usedClassLoader.getClassLoader(), (List<Task>) taskList, dataProvider);
  }

  /**
   * Cleanup method invoked when all tasks for the current bundle have completed.
   */
  @SuppressWarnings("unchecked")
  private void cleanup() {
    bundle.setParameter(BundleParameter.NODE_BUNDLE_ELAPSED_PARAM, accumulatedElapsed.get());
    node.getLifeCycleEventHandler().fireJobEnding(bundle, (AbstractJPPFClassLoader) usedClassLoader.getClassLoader(), (List<Task>) taskList, dataProvider);
    this.dataProvider = null;
    usedClassLoader.dispose();
    usedClassLoader = null;
    this.bundle = null;
    this.taskList = null;
    this.uuidList = null;
    setJobCancelled(false);
    this.taskWrapperList = null;
    timeoutHandler.clear();
  }

  /**
   * Notification sent by a node task wrapper when a task is complete.
   * @param taskWrapper the task that just ended.
   * @exclude
   */
  private void taskEnded(final NodeTaskWrapper taskWrapper) {
    long elapsedTime = taskWrapper.getElapsedTime();
    accumulatedElapsed.addAndGet(elapsedTime);
    NodeExecutionInfo info = taskWrapper.getExecutionInfo();
    long cpuTime = (info == null) ? 0L : (info.cpuTime / 1000000L);
    Task task = taskWrapper.getTask();
    TaskExecutionEvent event = new TaskExecutionEvent(task, getCurrentJobId(), cpuTime, elapsedTime/1000000L, task.getException() != null);
    for (TaskExecutionListener listener : taskExecutionListeners) listener.taskExecuted(event);
  }

  /**
   * Get the id of the job currently being executed.
   * @return the job id as a string, or null if no job is being executed.
   */
  public String getCurrentJobId() {
    return (bundle != null) ? bundle.getUuid() : null;
  }

  /**
   * Add a task execution listener to the list of task execution listeners.
   * @param listener the listener to add.
   */
  public void addTaskExecutionListener(final TaskExecutionListener listener) {
    taskExecutionListeners.add(listener);
  }

  /**
   * Remove a task execution listener from the list of task execution listeners.
   * @param listener the listener to remove.
   */
  public void removeTaskExecutionListener(final TaskExecutionListener listener) {
    taskExecutionListeners.remove(listener);
  }

  /**
   * Get the executor used by this execution manager.
   * @return an <code>ExecutorService</code> instance.
   */
  public ExecutorService getExecutor() {
    return threadManager.getExecutorService();
  }

  /**
   * Determines whether the configuration has changed and resets the flag if it has.
   * @return true if the config was changed, false otherwise.
   */
  public boolean checkConfigChanged() {
    return configChanged.compareAndSet(true, false);
  }

  /**
   * Trigger the configuration changed flag.
   */
  public void triggerConfigChanged() {
    configChanged.compareAndSet(false, true);
  }

  /**
   * Set the notification that the node must reconnect to the driver.
   * @param reconnectionNotification a {@link JPPFNodeReconnectionNotification} instance.
   * @exclude
   */
  public void setReconnectionNotification(final JPPFNodeReconnectionNotification reconnectionNotification) {
    this.reconnectionNotification.compareAndSet(null, reconnectionNotification);
  }

  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   */
  public void setThreadPoolSize(final int size) {
    if (size <= 0) {
      log.warn("ignored attempt to set the thread pool size to 0 or less: " + size);
      return;
    }
    int oldSize = getThreadPoolSize();
    threadManager.setPoolSize(size);
    int newSize = getThreadPoolSize();
    if (oldSize != newSize) {
      log.info("Node thread pool size changed from " + oldSize + " to " + size);
      JPPFConfiguration.getProperties().setProperty("processing.threads", Integer.toString(size));
      triggerConfigChanged();
    }
  }

  /**
   * Get the size of the node's thread pool.
   * @return the size as an int.
   */
  public int getThreadPoolSize() {
    return threadManager.getPoolSize();
  }

  /**
   * Get the priority assigned to the execution threads.
   * @return the priority as an int value.
   */
  public int getThreadsPriority() {
    return threadManager.getPriority();
  }

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   */
  public void updateThreadsPriority(final int newPriority) {
    threadManager.setPriority(newPriority);
  }

  /**
   * Get the thread manager for this node.
   * @return a {@link ThreadManager} instance.
   */
  public ThreadManager getThreadManager() {
    return threadManager;
  }

  /**
   * Determine whether the current job has been cancelled, including before starting its execution.
   * @return <code>true</code> if the job has been cancelled, <code>false</code> otherwise.
   */
  public boolean isJobCancelled() {
    return jobCancelled.get();
  }

  /**
   * Specify whether the current job has been cancelled, including before starting its execution.
   * @param jobCancelled <code>true</code> if the job has been cancelled, <code>false</code> otherwise.
   */
  public void setJobCancelled(final boolean jobCancelled) {
    this.jobCancelled.set(jobCancelled);
  }

  /**
   * Get the bundle whose tasks are currently being executed.
   * @return a {@link JPPFTaskBundle} instance.
   */
  public JPPFTaskBundle getBundle() {
    return bundle;
  }

  /**
   * Set the bundle whose tasks are currently being executed.
   * @param bundle a {@link JPPFTaskBundle} instance.
   */
  public void setBundle(final JPPFTaskBundle bundle) {
    this.bundle = bundle;
  }
}
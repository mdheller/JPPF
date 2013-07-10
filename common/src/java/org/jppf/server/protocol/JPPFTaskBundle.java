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
package org.jppf.server.protocol;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.node.protocol.*;
import org.jppf.utils.TraversalList;

/**
 * Instances of this class group tasks from the same client together, so they are sent to the same node,
 * avoiding unnecessary transport overhead.<br>
 * The goal is to provide a performance enhancement through an adaptive bundling of tasks originating from the same client.
 * The bundle size is computed dynamically, depending on the number of nodes connected to the server, and other factors.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFTaskBundle implements Serializable, Comparable<JPPFTaskBundle>, JPPFDistributedJob {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Type safe enumeration for the values of the bundle state.
   */
  public enum State {
    /**
     * Means the bundle is used for handshake with the server (initial bundle).
     */
    INITIAL_BUNDLE,
    /**
     * Means the bundle is used normally, to transport executable tasks.
     */
    EXECUTION_BUNDLE
  }

  /**
   * The unique identifier for the request (the job) this task bundle is a part of.
   */
  private String jobUuid = null;
  /**
   * Uuid of the original task bundle that triggered this resource request.
   */
  private String requestUuid = null;
  /**
   * The user-defined display name for this job.
   */
  private String name = null;
  /**
   * The unique identifier for the submitting application.
   */
  private TraversalList<String> uuidPath = new TraversalList<String>();
  /**
   * The number of tasks in this bundle at the time it is received by a driver.
   */
  protected int driverQueueTaskCount = 0;
  /**
   * The number of tasks in this bundle.
   */
  protected int taskCount = 0;
  /**
   * The current number of tasks in this bundle.
   */
  protected int currentTaskCount = 0;
  /**
   * The initial number of tasks in this bundle.
   */
  protected int initialTaskCount = 0;
  /**
   * The time at which this wrapper was added to the queue.
   */
  private transient long queueEntryTime = 0L;
  /**
   * The task completion listener to notify, once the execution of this task has completed.
   */
  private transient TaskCompletionListener completionListener = null;
  /**
   * The time it took a node to execute this task.
   */
  private long nodeExecutionTime = 0L;
  /**
   * The time at which the bundle is taken out of the queue fir sending to a node.
   */
  private long executionStartTime = 0L;
  /**
   * The state of this bundle, to indicate whether it is used for handshake with
   * the server or for transporting tasks to execute.
   */
  private State state = State.EXECUTION_BUNDLE;
  /**
   * Map holding the parameters of the request.
   */
  private final Map<Object, Object> parameters = new HashMap<Object, Object>();
  /**
   * The service level agreement between the job and the server.
   */
  private JobSLA jobSLA = new JPPFJobSLA();
  /**
   * The user-defined metadata associated with this job.
   */
  private JobMetadata jobMetadata = new JPPFJobMetadata();
  /**
   * Count of bundles copied.
   */
  private static AtomicLong copyCount = new AtomicLong(0);

  /**
   * Initialize this task bundle and set its build number.
   */
  public JPPFTaskBundle() {
  }

  /**
   * Get the unique identifier for the request this task is a part of.
   * @return the request uuid as a string.
   */
  public String getRequestUuid() {
    return (requestUuid == null) ? getUuid() : requestUuid;
  }

  /**
   * Set the unique identifier for the request this task is a part of.
   * @param requestUuid the request uuid as a string.
   */
  public void setRequestUuid(final String requestUuid) {
    this.requestUuid = requestUuid;
  }

  /**
   * Get the uuid path of the applications (driver or client) in whose classpath the class definition may be found.
   * @return the uuid path as a list of string elements.
   */
  public TraversalList<String> getUuidPath() {
    return uuidPath;
  }

  /**
   * Set the uuid path of the applications (driver or client) in whose classpath the class definition may be found.
   * @param uuidPath the uuid path as a list of string elements.
   */
  public void setUuidPath(final TraversalList<String> uuidPath) {
    this.uuidPath = uuidPath;
  }

  /**
   * Get the time at which this wrapper was added to the queue.
   * @return the time in milliseconds as a long value.
   */
  public long getQueueEntryTime() {
    return queueEntryTime;
  }

  /**
   * Set the time at which this wrapper was added to the queue.
   * @param queueEntryTime the time in milliseconds as a long value.
   */
  public void setQueueEntryTime(final long queueEntryTime) {
    this.queueEntryTime = queueEntryTime;
  }

  /**
   * Get the time it took a node to execute this task.
   * @return the time in milliseconds as a long value.
   */
  public long getNodeExecutionTime() {
    return nodeExecutionTime;
  }

  /**
   * Set the time it took a node to execute this task.
   * @param nodeExecutionTime the time in milliseconds as a long value.
   */
  public void setNodeExecutionTime(final long nodeExecutionTime) {
    this.nodeExecutionTime = nodeExecutionTime;
  }

  /**
   * Get the number of tasks in this bundle.
   * @return the number of tasks as an int.
   */
  public int getTaskCount() {
    return taskCount;
  }

  /**
   * Set the number of tasks in this bundle.
   * @param taskCount the number of tasks as an int.
   */
  public void setTaskCount(final int taskCount) {
    this.taskCount = taskCount;
    if (initialTaskCount <= 0) initialTaskCount = taskCount;
    if (currentTaskCount <= 0) currentTaskCount = taskCount;
  }

  /**
   * Set the initial number of tasks in this bundle.
   * @param initialTaskCount the number of tasks as an int.
   */
  public void setInitialTaskCount(final int initialTaskCount) {
    this.initialTaskCount = initialTaskCount;
  }

  /**
   * Get the task completion listener to notify, once the execution of this task has completed.
   * @return a <code>TaskCompletionListener</code> instance.
   */
  public TaskCompletionListener getCompletionListener() {
    return completionListener;
  }

  /**
   * Set the task completion listener to notify, once the execution of this task has completed.
   * @param listener a <code>TaskCompletionListener</code> instance.
   */
  public void setCompletionListener(final TaskCompletionListener listener) {
    this.completionListener = listener;
  }

  /**
   * Notifies that execution of this task has completed.
   * @param result the result of the task's execution.
   */
  public void fireTaskCompleted(final ServerJob result) {
    if (this.completionListener != null) this.completionListener.taskCompleted(result);
  }

  /**
   * Compare two task bundles, based on their respective priorities.<br>
   * <b>Note:</b> <i>this class has a natural ordering that is inconsistent with equals.</i>
   * @param bundle the bundle compare this one to.
   * @return a positive int if this bundle is greater, 0 if both are equal,
   * or a negative int if this bundle is less than the other.
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final JPPFTaskBundle bundle) {
    if (bundle == null) return 1;
    int otherPriority = bundle.getSLA().getPriority();
    if (jobSLA.getPriority() < otherPriority) return -1;
    if (jobSLA.getPriority() > otherPriority) return 1;
    return 0;
  }

  /**
   * Make a copy of this bundle.
   * @return a new <code>JPPFTaskBundle</code> instance.
   */
  public synchronized JPPFTaskBundle copy() {
    JPPFTaskBundle bundle = new JPPFTaskBundle();
    bundle.setUuidPath(uuidPath);
    bundle.setRequestUuid(getRequestUuid());
    bundle.setUuid(getUuid());
    bundle.setName(name);
    bundle.setTaskCount(taskCount);
    bundle.setCurrentTaskCount(currentTaskCount);
    bundle.initialTaskCount = initialTaskCount;
    synchronized(bundle.getParametersMap()) {
      for (Map.Entry<Object, Object> entry: parameters.entrySet()) bundle.setParameter(entry.getKey(), entry.getValue());
    }
    bundle.setQueueEntryTime(queueEntryTime);
    bundle.setCompletionListener(completionListener);
    bundle.setSLA(jobSLA);
    bundle.setMetadata(jobMetadata);
    bundle.setState(state);
    bundle.setParameter("bundle.uuid", uuidPath.getLast() + '-' + copyCount.incrementAndGet());
    return bundle;
  }

  /**
   * Make a copy of this bundle containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>JPPFTaskBundle</code> instance.
   */
  public JPPFTaskBundle copy(final int nbTasks) {
    JPPFTaskBundle bundle = copy();
    synchronized(this) {
      bundle.setTaskCount(nbTasks);
      taskCount -= nbTasks;
    }
    return bundle;
  }

  /**
   * Get the state of this bundle.
   * @return a <code>State</code> type safe enumeration value.
   */
  public State getState() {
    return state;
  }

  /**
   * Set the state of this bundle.
   * @param state a <code>State</code> type safe enumeration value.
   */
  public void setState(final State state) {
    this.state = state;
  }

  /**
   * Get the time at which the bundle is taken out of the queue for sending to a node.
   * @return the time as a long value.
   */
  public long getExecutionStartTime() {
    return executionStartTime;
  }

  /**
   * Set the time at which the bundle is taken out of the queue for sending to a node.
   * @param executionStartTime the time as a long value.
   */
  public void setExecutionStartTime(final long executionStartTime) {
    this.executionStartTime = executionStartTime;
  }

  /**
   * Get the initial task count of this bundle.
   * @return the task count as an int.
   */
  public int getInitialTaskCount() {
    return initialTaskCount;
  }

  /**
   * Set a parameter of this request.
   * @param name the name of the parameter to set.
   * @param value the value of the parameter to set.
   */
  public void setParameter(final Object name, final Object value) {
    synchronized(parameters) {
      parameters.put(name, value);
    }
  }

  /**
   * Get the value of a parameter of this request.
   * @param name the name of the parameter to get.
   * @return the value of the parameter, or null if the parameter is not set.
   */
  public Object getParameter(final Object name) {
    return parameters.get(name);
  }

  /**
   * Get the value of a parameter of this request.
   * @param name the name of the parameter to get.
   * @param defaultValue the default value to return if the parameter is not set.
   * @return the value of the parameter, or <code>defaultValue</code> if the parameter is not set.
   */
  public Object getParameter(final Object name, final Object defaultValue) {
    Object res = parameters.get(name);
    return res == null ? defaultValue : res;
  }

  /**
   * Remove a parameter from this request.
   * @param name the name of the parameter to remove.
   * @return the value of the parameter to remove, or null if the parameter is not set.
   */
  public Object removeParameter(final Object name) {
    return parameters.remove(name);
  }

  /**
   * Get the map holding the parameters of the request.
   * @return a map of string keys to object values.
   */
  public Map<Object, Object> getParametersMap() {
    return parameters;
  }

  @Override
  public JobSLA getSLA() {
    return jobSLA;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param jobSLA an instance of <code>JPPFJobSLA</code>.
   */
  public void setSLA(final JobSLA jobSLA) {
    this.jobSLA = jobSLA;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("name=").append(name);
    sb.append(", uuid=").append(jobUuid);
    sb.append(", initialTaskCount=").append(initialTaskCount);
    sb.append(", taskCount=").append(taskCount);
    sb.append(", bundleUuid=").append(getParameter("bundle.uuid"));
    sb.append(", uuidPath=").append(uuidPath);
    sb.append(']');
    return sb.toString();
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Set the user-defined display name for the job.
   * @param name the display name as a string.
   */
  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public JobMetadata getMetadata() {
    return jobMetadata;
  }

  /**
   * Set this bundle's metadata.
   * @param jobMetadata a {@link JPPFJobMetadata} instance.
   */
  public void setMetadata(final JobMetadata jobMetadata) {
    this.jobMetadata = jobMetadata;
  }

  @Override
  public String getUuid() {
    return jobUuid;
  }

  /**
   * Set the uuid of the initial job.
   * @param jobUuid the uuid as a string.
   */
  public void setUuid(final String jobUuid) {
    this.jobUuid = jobUuid;
  }

  /**
   * Get the current number of tasks in this bundle.
   * @return the current number of tasks as an int.
   */
  public int getCurrentTaskCount() {
    return currentTaskCount;
  }

  /**
   * Set the current number of tasks in this bundle.
   * @param currentTaskCount the current number of tasks as an int.
   */
  public void setCurrentTaskCount(final int currentTaskCount) {
    this.currentTaskCount = currentTaskCount;
  }

  /**
   * Get the bundle uuid.
   * @return the bundle uuid.
   */
  public Object getBundleUuid() {
    return getParameter("bundle.uuid");
  }

  /**
   * Get the job requeue flag.
   * @return job requeue flag.
   */
  public Object getRequeue() {
    return getParameter(BundleParameter.JOB_REQUEUE);
  }

  /**
   * Get the number of tasks in this bundle at the time it is received by a driver.
   * @return the number of tasks as an int.
   */
  public int getDriverQueueTaskCount() {
    return driverQueueTaskCount;
  }

  /**
   * Set the number of tasks in this bundle at the time it is received by a driver.
   * @param driverQueueTaskCount the number of tasks as an int.
   */
  public void setDriverQueueTaskCount(final int driverQueueTaskCount) {
    this.driverQueueTaskCount = driverQueueTaskCount;
  }
}
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

package test.org.jppf.test.setup;

import static org.junit.Assert.*;

import java.io.NotSerializableException;
import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.protocol.Task;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.junit.AfterClass;

import test.org.jppf.test.setup.BaseSetup.Configuration;
import test.org.jppf.test.setup.common.*;

/**
 * Base test setup for testing offline nodes.
 * @author Laurent Cohen
 */
public class AbstractSSLSetup
{
  /**
   * The jppf client to use.
   */
  protected static JPPFClient client = null;

  /**
   * Create the drivers and nodes configuration.
   * @param sslPrefix prefix to use to locate the configuration files
   * @return a {@link Configuration} instance.
   * @throws Exception if a process could not be started.
   */
  protected static Configuration createConfig(final String sslPrefix) throws Exception
  {
    Configuration testConfig = new Configuration();
    List<String> commonCP = new ArrayList<>();
    commonCP.add("classes/addons");
    commonCP.add("classes/tests/config");
    commonCP.add("../node/classes");
    commonCP.add("../JPPF/lib/slf4j/slf4j-api-1.6.1.jar");
    commonCP.add("../JPPF/lib/slf4j/slf4j-log4j12-1.6.1.jar");
    commonCP.add("../JPPF/lib/log4j/log4j-1.2.15.jar");
    commonCP.add("../JPPF/lib/jmxremote/jmxremote_optional-1.0_01-ea.jar");
    List<String> driverCP = new ArrayList<>(commonCP);
    driverCP.add("../common/classes");
    driverCP.add("../server/classes");
    String dir = "classes/tests/config/" + sslPrefix;
    testConfig.driverJppf = dir + "/driver.properties";
    testConfig.driverLog4j = "classes/tests/config/log4j-driver.template.properties";
    testConfig.driverClasspath = driverCP;
    testConfig.driverJvmOptions.add("-Djava.util.logging.testConfig.file=classes/tests/config/logging-driver.properties");
    testConfig.nodeJppf = dir + "/node.properties";
    testConfig.nodeLog4j = "classes/tests/config/log4j-node.template.properties";
    testConfig.nodeClasspath = commonCP;
    testConfig.nodeJvmOptions.add("-Djava.util.logging.testConfig.file=classes/tests/config/logging-node1.properties");
    System.setProperty("jppf.config", dir + "/client.properties");
    return testConfig;
  }

  /**
   * Stops the driver and nodes and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception
  {
    SSLHelper.resetConfig();
    BaseSetup.cleanup();
  }

  /**
   * Test that a simple job is normally executed.
   * @throws Exception if any error occurs
   */
  protected void testSimpleJob() throws Exception
  {
    int nbTasks = 5;
    String name = getClass().getSimpleName() + '.' + ReflectionUtils.getCurrentMethodName();
    JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 10L);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (Task<?> task: results)
    {
      assertTrue("task = " + task, task instanceof LifeCycleTask);
      Throwable t = task.getThrowable();
      assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }


  /**
   * Test multiple non-blocking jobs can be sent asynchronously.
   * @throws Exception if any error occurs
   */
  protected void testMultipleJobs() throws Exception
  {
    int nbTasks = 5;
    int nbJobs = 3;
    try
    {
      if (client != null) client.close();
      JPPFConfiguration.getProperties().setInt("jppf.pool.size", 2);
      client = BaseSetup.createClient(null, false);
      String name = getClass().getSimpleName() + '.' + ReflectionUtils.getCurrentMethodName();
      List<JPPFJob> jobs = new ArrayList<>(nbJobs);
      for (int i=1; i<=nbJobs; i++) jobs.add(BaseTestHelper.createJob(name + '-' + i, false, false, nbTasks, LifeCycleTask.class, 10L));
      for (JPPFJob job: jobs) client.submitJob(job);
      for (JPPFJob job: jobs)
      {
        JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
        List<Task<?>> results = collector.awaitResults();
        assertNotNull(results);
        assertEquals(nbTasks, results.size());
        for (Task<?> task: results)
        {
          assertTrue("task = " + task, task instanceof LifeCycleTask);
          Throwable t = task.getThrowable();
          assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
          assertNotNull(task.getResult());
          assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
        }
      }
    }
    finally
    {
      if (client != null) client.close();
      client = BaseSetup.createClient(null, true);
    }
  }

  /**
   * Test the cancellation of a job.
   * @throws Exception if any error occurs
   */
  protected void testCancelJob() throws Exception
  {
    int nbTasks = 10;
    JPPFJob job = BaseTestHelper.createJob("TestJPPFClientCancelJob", false, false, nbTasks, LifeCycleTask.class, 1000L);
    JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
    client.submitJob(job);
    Thread.sleep(1500L);
    client.cancelJob(job.getUuid());
    List<Task<?>> results = collector.awaitResults();
    assertNotNull(results);
    assertEquals("results size should be " + nbTasks + " but is " + results.size(), nbTasks, results.size());
    int count = 0;
    for (Task<?> task: results)
    {
      Throwable t = task.getThrowable();
      assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
      if (task.getResult() == null) count++;
    }
    assertTrue(count > 0);
  }

  /**
   * Test that a {@link java.io.NotSerializableException} occurring when a node returns execution results is properly handled.
   * @throws Exception if any error occurs
   */
  protected void testNotSerializableExceptionFromNode() throws Exception
  {
    int nbTasks = 2;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, NotSerializableTask.class, false);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (Task<?> task: results)
    {
      assertTrue(task instanceof NotSerializableTask);
      assertNull(task.getResult());
      assertNotNull(task.getThrowable());
      assertTrue(task.getThrowable() instanceof NotSerializableException);
    }
  }

  /**
   * Test that we can obtain the state of a node via the node forwarder mbean.
   * @throws Exception if nay error occurs.
   */
  protected void testForwardingMBean() throws Exception
  {
    JMXDriverConnectionWrapper driverJmx = BaseSetup.getDriverManagementProxy(client);
    JPPFNodeForwardingMBean nodeForwarder = driverJmx.getProxy(JPPFNodeForwardingMBean.MBEAN_NAME, JPPFNodeForwardingMBean.class);
    boolean ready = false;
    long elapsed = 0L;
    long start = System.currentTimeMillis();
    while (!ready)
    {
      elapsed = System.currentTimeMillis() - start;
      assertTrue((elapsed < 20_000L));
      try
      {
        Map<String, Object> result = nodeForwarder.state(NodeSelector.ALL_NODES);
        assertNotNull(result);
        assertEquals(BaseSetup.nbNodes(), result.size());
        for (Map.Entry<String, Object> entry: result.entrySet()) assertTrue(entry.getValue() instanceof JPPFNodeState);
        ready = true;
      }
      catch (Exception|AssertionError e)
      {
        Thread.sleep(100L);
      }
    }
    assertTrue(ready);
  }
}

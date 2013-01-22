/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
package test.jobfromtask;

import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * Instances of this class are defined as tasks with a predefined execution length, specified at their creation.
 * @author Laurent Cohen
 */
public class SourceTask extends JPPFTask
{
  /**
   * Initialize this task.
   */
  public SourceTask()
  {
  }

  /**
   * Perform the execution of this task.
   * @see sample.BaseDemoTask#doWork()
   */
  @Override
  public void run()
  {
    System.out.println("Starting source task '" + getId() + '\'');
    try
    {
      long start = System.nanoTime();
      print("submitting new remote job");
      String result = compute(new MyCallable());
      long elapsed = System.nanoTime() - start;
      String s = "processing  performed in "+StringUtils.toStringDuration(elapsed/1000000L);
      print(s);
      setResult(s);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      print("source task ended");
    }
  }

  /**
   * Called when this task is cancelled.
   * @see org.jppf.server.protocol.JPPFTask#onCancel()
   */
  @Override
  public void onCancel()
  {
    String s = "task '" + getId() + "' has been cancelled";
    setResult(s);
    print(s);
  }

  /**
   * Print a message to the log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg)
  {
    //log.info(msg);
    System.out.println(msg);
  }

  /**
   * 
   */
  public static class MyCallable implements JPPFCallable<String>
  {
    @Override
    public String call() throws Exception
    {
      return JobFromTaskRunner.submitDestinationJob("from source callable");
    }
  }
}

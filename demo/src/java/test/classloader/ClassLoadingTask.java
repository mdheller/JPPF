/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package test.classloader;

import org.jppf.server.protocol.JPPFTask;

public class ClassLoadingTask extends JPPFTask
{

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    try
    {
      /*
      AbstractJPPFClassLoader jppfClassLoader = (AbstractJPPFClassLoader) getClass().getClassLoader();
      System.out.println("jppfClassLoader = " + jppfClassLoader);
      Class c = jppfClassLoader.loadClass("com.hazelcast.core.Hazelcast");
       */
      Class c = Class.forName("com.hazelcast.core.Hazelcast");
      String s = "loaded " + c + ", classloader = " + c.getClassLoader() + ", task class loader = " + getClass().getClassLoader();
      System.out.println(s);
      setResult(s);
    }
    catch (Exception e)
    {
      setException(e);
    }
  }

}

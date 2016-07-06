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
  private static FileFilter logFileFIlter = new FileFilter() {
    @Override
    public boolean accept(final File path) {
      if (path.isDirectory()) return false;
      String s = path.getName();
      return (s != null) && s.endsWith(".log") && !s.startsWith("jppf-client");
    }
  };

  /** */
  @ClassRule
  public static TestWatcher classWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      print("***** start of class %s *****", description.getClassName());
      File dir = new File(System.getProperty("user.dir"));
      File[] logFiles = dir.listFiles(logFileFIlter);
      for (File file: logFiles) {
        if (file.exists()) {
          if (!file.delete()) System.err.printf("Could not delete %s%n", file);
        }
      }
    }

    @Override
    protected void finished(final Description description) {
      print("***** finished class %s() *****", description.getClassName());
      zipLogs(description.getClassName());
    }
  };

  /** */
  @Rule
  public TestWatcher instanceWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      print("***** start of method %s() *****", description.getMethodName());
    }
  };

  /**
   *
   * @param className the name of the class for which to zip the logs.
   */
  private static void zipLogs(final String className) {
    File dir = new File(System.getProperty("user.dir"));
    File logDir = new File(dir, "logs/");
    if (!logDir.exists()) logDir.mkdirs();
    File outZip = new File(logDir, className + ".zip");
    File[] logFiles = dir.listFiles(logFileFIlter);
    String[] logPaths = new String[logFiles.length];
    for (int i=0; i<logFiles.length; i++) logPaths[i] = logFiles[i].getPath();
    ZipUtils.zipFile(outZip.getPath(), logPaths);
  }

  /**
   *
   * @param format the format.
   * @param params the parmaters values.
   */
  private static void print(final String format, final Object...params) {
    String message = String.format(format, params);
    System.out.println(message);
    log.info(message);
  }
}

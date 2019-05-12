/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.node.protocol;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public final class TaskThreadLocals {
  /**
   * Uuid of the original task bundle that triggered a resource loading request.
   */
  private static final ThreadLocal<String> requestUuid = new ThreadLocal<>();

  /**
   * Instantiation not permitted.
   */
  private TaskThreadLocals() {
  }

  /**
   * Get the uuid for the original task bundle that triggered this resource request.
   * @return the uuid as a string.
   */
  public static String getRequestUuid() {
    return requestUuid.get();
  }

  /**
   * Set the uuid for the original task bundle that triggered this resource request.
   * @param requestUuid the uuid as a string.
   * @exclude
   */
  public static void setRequestUuid(final String requestUuid) {
    TaskThreadLocals.requestUuid.set(requestUuid);
  }
}
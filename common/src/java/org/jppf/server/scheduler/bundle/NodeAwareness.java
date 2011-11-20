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

package org.jppf.server.scheduler.bundle;

import org.jppf.management.JPPFSystemInformation;

/**
 * Bundler implementations should implement this interface if they wish to have access to a node's configuration.
 * @author Laurent Cohen
 */
public interface NodeAwareness
{
  /**
   * Get the corresponding node's system information.
   * @return a {@link JPPFSystemInformation} instance.
   */
  JPPFSystemInformation getNodeConfiguration();
  /**
   * Set the corresponding node's system information.
   * @param nodeConfiguration a {@link JPPFSystemInformation} instance.
   */
  void setNodeConfiguration(JPPFSystemInformation nodeConfiguration);
}

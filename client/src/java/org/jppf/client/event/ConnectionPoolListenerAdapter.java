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

package org.jppf.client.event;

/**
 * This adapter class provides an empty implementation of each method in the {@link ConnectionPoolListener} interface.
 * @author Laurent Cohen
 */
public class ConnectionPoolListenerAdapter implements ConnectionPoolListener {
  @Override
  public void connectionPoolAdded(final ConnectionPoolEvent event) {
  }

  @Override
  public void connectionPoolRemoved(final ConnectionPoolEvent event) {
  }

  @Override
  public void connectionAdded(final ConnectionPoolEvent event) {
  }

  @Override
  public void connectionRemoved(final ConnectionPoolEvent event) {
  }
}

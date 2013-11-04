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

package org.jppf.client.concurrent;

import java.util.*;

import org.jppf.node.protocol.Task;

/**
 * An event generated by a {@link FutureResultCollector}.
 * @author Laurent Cohen
 * @exclude
 */
class FutureResultCollectorEvent extends EventObject
{
  /**
   * The results that were received.
   */
  private final List<Task<?>> results;

  /**
   * Initialize this event with the specified source.
   * @param collector the source of this event.
   * @param results the results that were received.
   */
  FutureResultCollectorEvent(final FutureResultCollector collector, final List<Task<?>> results)
  {
    super(collector);
    this.results = results;
  }

  /**
   * Get the FutureResultCollector source of this event.
   * @return a {@link FutureResultCollector} instance.
   */
  FutureResultCollector getCollector()
  {
    return (FutureResultCollector) getSource();
  }

  /**
   * Get the results that were received.
   * @return a list of <code>JPPFTask</code> instances.
   */
  List<Task<?>> getResults()
  {
    return results;
  }
}

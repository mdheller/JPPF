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

package org.jppf.admin.web.admin;

/**
 * 
 * @author Laurent Cohen
 */
public class AdminConfigConstants {
  /**
   * Save edit config as current config action id.
   */
  public static String SAVE_ACTION = "admin.config.save";
  /**
   * Revert to current config action id.
   */
  public static String REVERT_ACTION = "admin.config.revert";
  /**
   * Save the new config as current and reset the client action id.
   */
  public static String RESET_CLIENT_ACTION = "admin.config.reset_client";
  /**
   * Download the current config to file action id.
   */
  public static String DOWNLOAD_ACTION = "admin.config.download";
  /**
   * Upload the current config from file action id.
   */
  public static String UPLOAD_ACTION = "admin.config.upload";
  /**
   * Sort in ascending order action id.
   */
  public static String SORT_ASC_ACTION = "admin.config.sort_asc";
  /**
   * Sort in ascending order action id.
   */
  public static String SORT_DESC_ACTION = "admin.config.sort_desc";
}

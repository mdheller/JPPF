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

package test.org.jppf.test.setup;

import javax.naming.*;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;

import org.jppf.jca.cci.*;

/**
 * Utility class for obtaining and releasing Resource adapter connections.
 * @author Laurent Cohen
 */
public class JPPFHelper {
  /**
   * JNDI name of the connection factory for JBoss app server.
   */
  public static final String JNDI_NAME_JBOSS = "java:eis/JPPFConnectionFactory";
  /**
   * JNDI name of the connection factory for Geronimo app server.
   */
  public static final String JNDI_NAME_GERONIMO = "jca:/JPPF/jca-client/JCAConnectionManager/eis/JPPFConnectionFactory";
  /**
   * JNDI name of the JCA connection factory to use.
   */
  private static String jndiName = JNDI_NAME_JBOSS;

  /**
   * Obtain a JPPF connection from the resource adapter's connection pool.
   * The obtained connection must be closed by the caller of this method, once it is done using it.
   * @return a <code>JPPFConnection</code> instance.
   * @throws NamingException if the connection factory lookup failed.
   * @throws ResourceException if a connection could not be obtained.
   */
  public static JPPFConnection getConnection() throws NamingException, ResourceException {
    final InitialContext context = new InitialContext();
    final Object objref = context.lookup(jndiName);
    JPPFConnectionFactory cf;
    if (objref instanceof JPPFConnectionFactory) cf = (JPPFConnectionFactory) objref;
    else cf = (JPPFConnectionFactory) javax.rmi.PortableRemoteObject.narrow(objref, ConnectionFactory.class);
    return (JPPFConnection) cf.getConnection();
  }

  /**
   * Close (release) the specified connection.
   * @param connection the connection to close.
   * @throws ResourceException if the connection could not be closed.
   */
  public static void closeConnection(final JPPFConnection connection) throws ResourceException {
    connection.close();
  }

  /**
   * Get the JNDI name of the JCA connection factory to use.
   * @return the JNDI name as a string.
   */
  public static String getJndiName() {
    return jndiName;
  }

  /**
   * Set the JNDI name of the JCA connection factory to use.
   * @param jndiName the JNDI name as a string.
   */
  public static void setJndiName(final String jndiName) {
    JPPFHelper.jndiName = jndiName;
  }
}

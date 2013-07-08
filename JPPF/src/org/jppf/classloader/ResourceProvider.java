/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.classloader;

import java.io.*;
import org.apache.log4j.Logger;

/**
 * 
 * @author Laurent Cohen
 */
public class ResourceProvider
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ResourceProvider.class);
	/**
	 * Maximum buffer size for reading class files.
	 */
	private static final int BUFFER_SIZE = 32*1024;
	/**
	 * Temporary buffer used to read class files.
	 */
	protected byte[] buffer = new byte[BUFFER_SIZE];

	/**
	 * Load a resource file (including class files) from the class path into an array of byte.
	 * @param resName the name of the resource to load.
	 * @return an array of bytes, or nll if the resource could not be found.
	 */
	public byte[] getResourceAsBytes(String resName)
	{
		byte[] b = null;
		try
		{
			InputStream is = getClass().getClassLoader().getResourceAsStream(resName);
			if (is == null)
			{
				File file = new File(resName);
				if (file.exists()) is = new BufferedInputStream(new FileInputStream(file));
			}
			if (is != null)
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				boolean end = false;
				int offset = 0;
				while (!end)
				{
					int n = is.read(buffer, 0, BUFFER_SIZE);
					if (n < 0) break;
					if (n < BUFFER_SIZE) end = true;
					baos.write(buffer, 0, n);
					offset += n;
				}
				is.close();
				baos.flush();
				b = baos.toByteArray();
				baos.close();
			}
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return b;
	}
}
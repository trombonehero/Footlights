/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.footlights.boot;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.jar.JarFile;


/** Opens a {@link JarFile}, or on failure, provides a means to return an {@link Exception}. */
class JAROpener implements PrivilegedAction<JarFile>
{
	synchronized JarFile open(URL url) throws IOException
	{
		if (url.toExternalForm().startsWith("jar:")) this.url = url;
		else this.url = new URL("jar:" + url + "!/");

		JarFile jar = AccessController.doPrivileged(this);

		if (jar == null) throw error;
		else return jar;
	}

	@Override public synchronized JarFile run()
	{
		try { return ((JarURLConnection) url.openConnection()).getJarFile(); }
		catch(IOException e)
		{
			error = e;
			return null;
		}
	}

	private URL url;
	private IOException error;
}

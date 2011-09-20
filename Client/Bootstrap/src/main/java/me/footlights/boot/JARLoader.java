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
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/** Loads classes from a single JAR file. */
public class JARLoader
{
	public JARLoader(URL url) throws IOException
	{
		this.url = url;

		if (url.toString().startsWith("jar:file:") || url.toString().startsWith("file:"))
		{
			String fileURL = url.toString();
			if (fileURL.startsWith("jar:file:"))
				fileURL = fileURL.replaceFirst("jar:file:", "");

			else if (fileURL.startsWith("file:"))
				fileURL = fileURL.replaceFirst("file:", "");


			if (fileURL.startsWith("///"))
				fileURL = fileURL.replaceFirst("///", "/");

			fileURL = fileURL.replaceFirst("!/$", "");

			jar = new JarFile(fileURL);
		}
		else
		{
			final URLConnection connection = url.openConnection();
			connection.setUseCaches(false);

			jar = 
				AccessController.doPrivileged(new PrivilegedAction<JarFile>()
				{
					public JarFile run()
					{
						try { return ((JarURLConnection) connection).getJarFile(); }
						catch(IOException e) { throw new Error(e); }
					}
				});
		}


		if (jar.getManifest() == null)
			throw new SecurityException("The jar file is not signed");
	}


	public JarFile getJarFile() { return jar; }


	/** Read a class' bytecode. */
	public Bytecode readBytecode(String className)
		throws ClassNotFoundException, IOException
	{
		for (Enumeration<JarEntry> i = jar.entries() ; i.hasMoreElements() ;)
		{
			JarEntry entry = i.nextElement();

			if (entry.isDirectory()) continue;
			if (entry.getName().startsWith("META-INF/")) continue;

			// read the JAR entry (to make sure it's actually signed)
        	InputStream is = jar.getInputStream(entry);
        	int avail = is.available();
    		byte[] buffer = new byte[avail];
    		is.read(buffer);
			is.close();

            if (entry.getName().equals(className.replace('.', '/') + ".class"))
            {

               if (entry.getCodeSigners() == null)
               	throw new Error(entry.toString() + " not signed");

            	String jarName = url.toExternalForm();
            	jarName = jarName.replaceFirst("jar:", "");
            	jarName = jarName.replace("!/", "");

            	Bytecode bytecode = new Bytecode();
            	bytecode.raw = buffer;
				bytecode.source = new CodeSource(new URL(jarName), entry.getCodeSigners());

            	return bytecode;
            }
		}

		throw new ClassNotFoundException();
	}


	/** The {@link JarFile} that we are loading classes from. */
	private final JarFile jar;

	/** Where {@link #jar} came from. */
	private final URL url;
}

package me.footlights.boot;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class JARLoader
{
	public JARLoader(URL url) throws IOException
	{
		this.url = url;

		if(url.toString().startsWith("jar:file:") || url.toString().startsWith("file:"))
		{
			String fileURL = url.toString();
			if(fileURL.startsWith("jar:file:"))
				fileURL = fileURL.replaceFirst("jar:file:", "");

			else if(fileURL.startsWith("file:"))
				fileURL = fileURL.replaceFirst("file:", "");


			if(fileURL.startsWith("///"))
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


		if(jar.getManifest() == null)
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

			if(entry.isDirectory()) continue;
			if(entry.getName().startsWith("META-INF/")) continue;

			// read the JAR entry (to make sure it's actually signed)
        	InputStream is = jar.getInputStream(entry);
        	int avail = is.available();
    		byte[] buffer = new byte[avail];
    		is.read(buffer);
			is.close();

            if(entry.getName().equals(className.replace('.', '/') + ".class"))
            {

               if(entry.getCodeSigners() == null)
               	throw new Error(entry.toString() + " not signed");

            	String jarName = url.toExternalForm();
            	jarName = jarName.replaceFirst("jar:", "");
            	jarName = jarName.replace("!/", "");

            	Bytecode bytecode = new Bytecode();
            	bytecode.raw = buffer;
            	bytecode.source = new CodeSource(
            		new URL(jarName), entry.getCodeSigners());

            	return bytecode;
            }
		}

		throw new ClassNotFoundException();
	}


	private URL url;
	private JarFile jar;
}

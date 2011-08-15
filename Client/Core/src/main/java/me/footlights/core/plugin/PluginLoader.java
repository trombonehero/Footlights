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
package me.footlights.core.plugin;

import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;

import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import me.footlights.core.Preconditions;
import me.footlights.plugin.Plugin;



public class PluginLoader extends ClassLoader
{
	public PluginLoader()
	{
		coreLoader       = getClass().getClassLoader();
		plugins          = Maps.newHashMap();
		pluginClasses    = Maps.newHashMap();
		packageURLs      = Maps.newHashMap();
	}


	public PluginWrapper loadPlugin(String name, URI uri, Logger log) throws PluginLoadException
	{
		try
		{
			if(plugins.containsKey(uri)) return plugins.get(uri);

			Class<?> c = loadClass(uri.toString());
			Plugin plugin = (Plugin) c.newInstance();

			return new PluginWrapper(name, uri, plugin, log);
		}
		catch(Exception e) { throw new PluginLoadException(uri, e); }
	}




	/**
	 * Find a class, given the class name.
	 * 
	 * @name   The name of the class, either in the usual format (package.Class)
	 *         or in Jar-like format:
	 *         jar:/http://host.com/path/to/plugin.jar!/packagename.Classname
	 */
	@Override protected synchronized Class<?> findClass(String name)
		throws ClassNotFoundException
	{
		// is this a core class?
		if(name.contains("me.footlights.core")) return coreLoader.loadClass(name);
		if(name.contains("me.footlights.ui")) return coreLoader.loadClass(name);


		// have we already loaded this plugin?
		if(pluginClasses.containsKey(name)) return pluginClasses.get(name);

		String[] parts = name.split("\\.jar!/");
		if(parts.length > 1 && pluginClasses.containsKey(parts[1]))
			return pluginClasses.get(parts[1]);


		// load it
		if(parts.length == 2)
		{
			try
			{
				URL jarURL = new URL(parts[0] + ".jar!/");
				String className = parts[1];
	
				return findPluginClass(jarURL, className);
			}
			catch(MalformedURLException e) { throw new Error(e); }
		}
		else if(parts.length == 1)
		{
			int lastDot = name.lastIndexOf(".");
			String packageName = name.substring(0, lastDot);
			URL packageURL = packageURLs.get(packageName);

			if (packageURL == null)
				throw new ClassNotFoundException(
					"Unknown package '" + packageName + "'");

			return findPluginClass(packageURLs.get(packageName), name);
		}
		else throw new ClassNotFoundException("Invalid plugin class: " + name);
	}


	/**
	 * Find a plugin class from a Jar file.
	 * 
	 * @param   url            the Jar's URL (starts with jar:/, etc.)
	 * @param   className      the class' name
	 */
	public synchronized Class<?> findPluginClass(URL url, String className)
		throws ClassNotFoundException
	{
		Preconditions.notNull(url);

		// first, get the bytecode
		Bytecode bytecode = readBytecodeFromJAR(url, className);
		String sourceURL = bytecode.source.getLocation().toString();
		if(sourceURL.startsWith("file:"))
			sourceURL = sourceURL.replace("file:", "");

		Permissions permissions = new Permissions();
		permissions.add(new FilePermission(sourceURL, "read"));

		ProtectionDomain domain
			= new ProtectionDomain(bytecode.source, permissions);

		int lastDot = className.lastIndexOf(".");
		String packageName = className.substring(0, lastDot);
		packageURLs.put(packageName, url);

		Class<?> c = defineClass(className, bytecode.raw,
		                         0, bytecode.raw.length, domain);

		pluginClasses.put(className, c);

		return c;
	}



	/**
	 * Read bytecode from a JAR file.
	 * 
	 * @param   url           URL of the JAR file to read from
	 * @param   className     the name of the class whose bytecode we're loading
	 * 
	 * @return  a Bytecode object
	 * 
	 * @throws  ClassNotFoundException
	 */
	Bytecode readBytecodeFromJAR(URL url, String className)
		throws ClassNotFoundException
	{
		JarFile jar = null;

		try
		{
			jar = getJarFile(url);

			Manifest man = jar.getManifest();
			if (man == null)
				throw new SecurityException("The jar file is not signed");

			// load our entry
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

			throw new ClassNotFoundException(
				jar + " does not contain " + className);
		}
		catch (IOException e)
		{
			throw new ClassNotFoundException(
				"Error loading " + className + " from URL " + url, e);
		}
	}



	/**
	 * Turn a URL into a Jar file.
	 */
	private JarFile getJarFile(URL url) throws IOException
	{
		Preconditions.notNull(url);

		JarFile jar;

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
		return jar;
	}


	/** Loads core classes */
	private final ClassLoader coreLoader;

	/** Plugins we've already loaded */
	private Map<URI,PluginWrapper> plugins;
	private Map<String,Class<?>> pluginClasses;
	private Map<String,URL> packageURLs;
}

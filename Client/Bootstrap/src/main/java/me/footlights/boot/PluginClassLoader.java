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

import java.io.FilePermission;
import java.io.IOException;
import java.net.URL;

import java.security.Permissions;
import java.security.ProtectionDomain;

import java.util.Map;

import com.google.common.collect.Maps;


/** Loads unprivileged plugin code. */
class PluginClassLoader extends ClassLoader
{
	public PluginClassLoader(FootlightsClassLoader coreLoader)
	{
		this.coreLoader  = coreLoader;
		pluginClasses    = Maps.newHashMap();
		pluginJars      = Maps.newHashMap();
	}


	/**
	 * Find a plugin class, given the class name or an explicit JAR file + class name.
	 * 
	 * @name   The name of the class, either in the usual format (package.Class)
	 *         or in Jar-like format:
	 *         jar:/http://host.com/path/to/plugin.jar!/packagename.Classname.
	 *         If using the usual package.Class format with implicit JAR file, another class
	 *         must have been previously loaded from the same package with an explicit JAR file. 
	 */
	@Override protected synchronized Class<?> findClass(String name)
		throws ClassNotFoundException
	{
		if (name.startsWith("me.footlights")) return coreLoader.findClass(name);

		// Have we already loaded this plugin?
		if (pluginClasses.containsKey(name)) return pluginClasses.get(name);

		// Find the JAR file, whether it's given implicitly or explicitly, and class name.
		final JARLoader jar;
		String className;

		String[] parts = name.split("\\.jar!/");
		if (parts.length == 2)
		{
			// Both the JAR file and class name have been given explicitly; open the JAR file.
			try { jar = JARLoader.open(new URL(parts[0] + ".jar")); }
			catch (IOException e) { throw new ClassNotFoundException("Error reading JAR", e); }

			className = parts[1];
		}
		else if (parts.length == 1)
		{
			// Only the class name is explicit; the JAR file must be implicit (and already open).
			className = parts[0];

			int lastDot = name.lastIndexOf(".");
			String packageName = name.substring(0, lastDot);
			jar = pluginJars.get(packageName);

			if (jar == null)
				throw new ClassNotFoundException("Unknown package '" + packageName + "'");
		}
		else throw new ClassNotFoundException("Invalid class name: '" + name + "'");


		// Read and define the class.
		Bytecode bytecode;
		try { bytecode = jar.readBytecode(className); }
		catch (IOException e) { throw new ClassNotFoundException("Error reading JAR", e); }

		String sourceURL = bytecode.source.getLocation().toString();
		if (sourceURL.startsWith("file:"))
			sourceURL = sourceURL.replace("file:", "");

		Permissions permissions = new Permissions();
		permissions.add(new FilePermission(sourceURL, "read"));
		ProtectionDomain domain = new ProtectionDomain(bytecode.source, permissions);

		int lastDot = className.lastIndexOf(".");
		String packageName = className.substring(0, lastDot);
		pluginJars.put(packageName, jar);

		Class<?> c = defineClass(className, bytecode.raw, 0, bytecode.raw.length, domain);
		pluginClasses.put(className, c);

		return c;
	}


	/** {@link ClassLoader} for core Footlights classes. */
	private final FootlightsClassLoader coreLoader;

	/** Plugins we've already loaded */
	private final Map<String, Class<?>> pluginClasses;

	/** Plugin JAR files. */
	private final Map<String, JARLoader> pluginJars;
}

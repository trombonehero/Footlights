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

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.Permissions;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;


/** Loads "core" code (footlights.core.*, footlights.ui.*) from a known source */
class FootlightsClassLoader extends ClassLoader
{
	/** Constructor */
	public FootlightsClassLoader(Iterable<URL> classpaths)
		throws MalformedURLException
	{
		this.classpaths = Iterables.unmodifiableIterable(classpaths);
		this.knownPackages = Maps.newLinkedHashMap();

		corePermissions = new Permissions();
		corePermissions.add(new AllPermission());
		corePermissions.setReadOnly();
	}


	@Override protected synchronized Class<?> loadClass(String name, boolean resolve)
		throws ClassNotFoundException
	{
		Class<?> c = findClass(name);
		if (resolve) resolveClass(c);

		return c;
	}


	/** Find a core Footlights class */
	@Override protected synchronized Class<?> findClass(String name)
		throws ClassNotFoundException
	{
		// If the "class name" is very specially encoded, we actually want to load a plugin.
		if (name.contains("!/"))
		{
			String[] tokens = name.split("!/");
			if (tokens.length != 2)
				throw new ClassNotFoundException("Invalid class name: '" + name + "'");

			URL classpath;
			try
			{
				if (tokens[0].startsWith("jar")) classpath = new URL(tokens[0] + "!/");
				else classpath = new URL(tokens[0]);
			}
			catch (MalformedURLException e)
			{
				throw new ClassNotFoundException("Invalid classpath: " + tokens[0], e);
			}

			String className = tokens[1];
			String packageName = className.substring(0, className.lastIndexOf("."));

			try
			{
				return ClasspathLoader.create(this, classpath, packageName).loadClass(className);
			}
			catch (FileNotFoundException e)
			{
				throw new ClassNotFoundException("Invalid classpath: " + classpath, e);
			}
		}

		// We must be loading a core Footlights class or a Java library class.
		if (!name.startsWith("me.footlights"))
			return getParent().loadClass(name);

		// Do we already know what classpath to find the class in?
		String packageName = name.substring(0, name.lastIndexOf('.'));
		ClassLoader packageLoader = knownPackages.get(packageName);
		if (packageLoader != null)
			return packageLoader.loadClass(name);

		// Search known package sources.
		for (String prefix : knownPackages.keySet())
			if (packageName.startsWith(prefix))
				return knownPackages.get(prefix).loadClass(name);

		// Fall back to exhaustive search of core classpaths.
		for (URL url : classpaths)
		{
			try
			{
				ClasspathLoader loader = ClasspathLoader.create(this, url, packageName);
				Class<?> c = loader.loadClass(name);
				knownPackages.put(packageName, loader);
				return c;
			}
			catch (Exception e) {}
		}

		throw new ClassNotFoundException("No " + name + " in " + classpaths);
	}


	/** Cached permissions given to core classes. */
	private final Permissions corePermissions;

	/** Where we can find core classes. */
	private final Iterable<URL> classpaths;

	/** Mapping of packages to classpaths. */
	private final Map<String, ClassLoader> knownPackages;
}

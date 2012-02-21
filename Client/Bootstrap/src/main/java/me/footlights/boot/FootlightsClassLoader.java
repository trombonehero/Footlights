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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import scala.Option;

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
	}


	@Override protected synchronized Class<?> loadClass(String name, boolean resolve)
		throws ClassNotFoundException
	{
		if (!(name.startsWith("me.footlights") || name.contains("!/")))
			return getParent().loadClass(name);

		Class<?> c = findLoadedClass(name);
		if (c == null) c = findClass(name);
		if (resolve) resolveClass(c);

		return c;
	}


	/** Find a core Footlights class */
	@Override protected synchronized Class<?> findClass(String name)
		throws ClassNotFoundException
	{
		// We must be loading a core Footlights class.
		if (!name.startsWith("me.footlights"))
			throw new IllegalArgumentException(
					FootlightsClassLoader.class.getSimpleName() +
					".findClass() is only used directly for loading core Footlights classes, not" +
					name);

		// Do we already know what classpath to find the class in?
		String packageName = name.substring(0, name.lastIndexOf('.'));
		ClasspathLoader packageLoader = knownPackages.get(packageName);
		if (packageLoader != null)
			return packageLoader.loadClass(name, true);

		// Search known package sources.
		for (String prefix : knownPackages.keySet())
			if (packageName.startsWith(prefix))
			{
				Option<Class<?>> c = knownPackages.get(prefix).findInClasspath(name);
				if (c.isDefined()) return c.get();
			}

		// Fall back to exhaustive search of core classpaths.
		for (URL url : classpaths)
		{
			ClasspathLoader loader = ClasspathLoader.create(this, url, packageName);
			Option<Class<?>> c = loader.findInClasspath(name);
			if (c.isEmpty()) continue;

			knownPackages.put(packageName, loader);
			return c.get();
		}

		throw new ClassNotFoundException("No " + name + " in " + classpaths);
	}


	/**
	 * Load an unprivileged application.
	 *
	 * In the future, we will simply specify a class path and let the manifest tell us what
	 * class to execute. In the meantime, however, we want to be minimally disruptive.
	 */
	Class<?> loadApplication(final URL classpath, final String className)
			throws ClassNotFoundException
	{
		String packageName = className.substring(0, className.lastIndexOf("."));
		return ClasspathLoader.create(this, classpath, packageName).loadClass(className);
	}

	/** Where we can find core classes. */
	private final Iterable<URL> classpaths;

	/** Mapping of (core) packages to classpaths. */
	private final Map<String, ClasspathLoader> knownPackages;
}

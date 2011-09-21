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

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
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
		if (!name.startsWith("me.footlights"))
			return super.loadClass(name, resolve);

		Class<?> c = findClass(name);
		if (resolve) resolveClass(c);

		return c;
	}


	/** Find a core Footlights class */
	@Override protected synchronized Class<?> findClass(String name)
		throws ClassNotFoundException
	{
		// If we're not loading from the 'me.footlights' package, treat as a plugin:
		// load from anywhere we're asked to, but apply security restrictions.
		boolean privileged =
			(name.startsWith("me.footlights.core") || name.startsWith("me.footlights.ui"));

		// If the "class name" is very specially encoded, we actually want to load a plugin.
		if (name.contains("!/"))
		{
			privileged = false;

			String[] tokens = name.split("!/");
			if (tokens.length != 2)
				throw new ClassNotFoundException("Invalid class name: '" + name + "'");

			try { return findClass(new URL(tokens[0] + "!/"), tokens[1], privileged); }
			catch (IOException e)
			{
				throw new ClassNotFoundException("Unable to load '" + name + "'", e);
			}
		}

		// Do we already know what classpath to find the class in?
		String packageName = name.substring(0, name.lastIndexOf('.'));
		URL packageUrl = knownPackages.get(packageName);
		if (packageUrl != null)
			try { return findClass(packageUrl, name, privileged); }
			catch(IOException e)
			{
				throw new ClassNotFoundException("Error reading from '" + packageUrl + "'", e);
			}

		// Fall back to exhaustive search.
		for (URL url : classpaths)
			try { return findClass(url, name, privileged); }
			catch(ClassNotFoundException e) {}
			catch(IOException e) {}

		throw new ClassNotFoundException("No " + name + " in " + classpaths);
	}


	/**
	 * Find a class, which may or may not be privileged.
	 *
	 * @param  privileged     If true, the class will be granted the {@link AllPermission},
	 *                        allowing it to access arbitrary files, open sockets, etc.
	 *                        If false, the class will be granted permission to read its own
	 *                        classpath (and thus open its own resources).
	 *                        Obviously, this should only be set true for core Footlights code.
	 */
	private Class<?> findClass(URL classpath, String name, boolean privileged)
		throws ClassNotFoundException, IOException
	{
		Bytecode bytecode = Bytecode.read(classpath, name);

		PermissionCollection perms;
		if (privileged) perms = corePermissions;
		else
		{
			perms = new Permissions();
			perms.add(new FilePermission(classpath.toExternalForm(), "read"));
		}

		ProtectionDomain domain = new ProtectionDomain(bytecode.source, perms);

		String packageName = name.substring(0, name.lastIndexOf('.'));
		knownPackages.put(packageName, classpath);

		return defineClass(name, bytecode.raw, 0, bytecode.raw.length, domain);
	}

	@Override public synchronized URL findResource(String name)
	{
		for (URL url : classpaths)
		{
			try
			{
				URL bigURL = new URL(url.toString() + "/" + name);
				if (new File(bigURL.getFile()).exists()) return bigURL;
			}
			catch(MalformedURLException e) { throw new Error(e); }
		}
		return super.findResource(name);
	}


	/** Cached permissions given to core classes. */
	private final Permissions corePermissions;

	/** Where we can find core classes. */
	private final Iterable<URL> classpaths;

	/** Mapping of packages to classpaths. */
	private final Map<String, URL> knownPackages;
}

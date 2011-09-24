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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Map;

import com.google.common.collect.Maps;


/** Loads classes and resources from a single classpath. */
class ClasspathLoader extends ClassLoader
{
	static ClasspathLoader create(ClassLoader parent, URL classpath, String basePackage)
	{
		// Only grant privileges to code Footlights code.
		final PermissionCollection permissions;
		if (basePackage.startsWith("me.footlights.core")
		    || basePackage.startsWith("me.footlights.ui"))
			permissions = PRIVILEGED_PERMISSIONS;
		else
		{
			permissions = new Permissions();
			permissions.add(new FilePermission(classpath.toExternalForm(), "read"));
			permissions.setReadOnly();
		}

		return new ClasspathLoader(parent, classpath, basePackage, permissions);
	}


	@Override public synchronized Class<?> loadClass(String name)
		throws ClassNotFoundException
	{
		return loadClass(name, false);
	}

	@Override protected synchronized Class<?> loadClass(String name, boolean resolve)
		throws ClassNotFoundException
	{
		if (!name.startsWith(basePackage))
			return getParent().loadClass(name);

		Class<?> c = findClass(name);
		if (resolve) resolveClass(c);

		return c;
	}


	/** Find a core Footlights class */
	@Override protected synchronized Class<?> findClass(final String name)
		throws ClassNotFoundException
	{
		if (loadedClasses.containsKey(name)) return loadedClasses.get(name);

		Bytecode bytecode;
		try
		{
			bytecode = AccessController.doPrivileged(new PrivilegedExceptionAction<Bytecode>()
				{
					@Override
					public Bytecode run() throws ClassNotFoundException, IOException
					{
						return Bytecode.read(classpath, name);
					}
				});
		}
		catch (PrivilegedActionException e)
		{
			throw new ClassNotFoundException("Unable to load " + name + " from " + classpath, e);
		}

		ProtectionDomain domain = new ProtectionDomain(bytecode.source, permissions);
		Class<?> c = defineClass(name, bytecode.raw, 0, bytecode.raw.length, domain);
		loadedClasses.put(name, c);
		return c;
	}


	@Override public synchronized URL findResource(String name)
	{
		try { return new URL(classpath.toString() + "/" + name); }
		catch(MalformedURLException e) { return null; }
	}


	@Override public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("ClasspathLoader { package = '");
		sb.append(basePackage);
		sb.append("', url = '");
		sb.append(classpath.toExternalForm());
		sb.append("' }");

		return sb.toString();
	}

	/**
	 * Constructor.
	 *
	 * @param parent            Parent ClassLoader.
	 * @param classpath         The classpath (JAR or directory) that we are loading from
	 * @param basePackage       The base package that we are responsible for. For instance,
	 *                          if loading a plugin with packages com.foo.app and com.foo.support,
	 *                          this parameter should be "com.foo".
	 * @param permissions       Permissions that should be granted to loaded classes.
	 */
	private ClasspathLoader(ClassLoader parent, URL classpath, String basePackage,
		PermissionCollection permissions)
	{
		super(parent);

		this.classpath = classpath;
		this.basePackage = basePackage;
		this.permissions = permissions;

		this.loadedClasses = Maps.newHashMap();
	}


	/** Classes that we've already loaded. */
	private final Map<String, Class<?>> loadedClasses;

	/** Where we find our classes and resources. */
	private final URL classpath;

	/** The package that we are loading classes from. */
	private final String basePackage;

	/** Cached permissions given to classes that we load. */
	private final PermissionCollection permissions;

	/** Permissions given to privileged code. */
	private static final PermissionCollection PRIVILEGED_PERMISSIONS;
	static
	{
		PRIVILEGED_PERMISSIONS = new Permissions();
		PRIVILEGED_PERMISSIONS.add(new AllPermission());
		PRIVILEGED_PERMISSIONS.setReadOnly();
	}
}

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
import java.io.FilePermission;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import scala.Option;
import scala.Some;
import scala.Tuple2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;


/** Loads classes and resources from a single classpath. */
class ClasspathLoader extends ClassLoader
{
	static ClasspathLoader create(ClassLoader parent, URL path, String basePackage)
		throws FileNotFoundException, MalformedURLException
	{
		// Only grant privileges to core Footlights code.
		final PermissionCollection permissions;
		if (basePackage.startsWith("me.footlights.core")
		    || basePackage.startsWith("me.footlights.ui"))
			permissions = PRIVILEGED_PERMISSIONS;
		else
		{
			permissions = new Permissions();
			permissions.add(new FilePermission(path.toExternalForm(), "read"));
			permissions.setReadOnly();
		}

		if (!path.getProtocol().startsWith("jar:") && path.getPath().endsWith(".jar"))
			path = new URL("jar:" + path + "!/");

		Classpath classpath = Classpath.open(path, basePackage).get();
		if (classpath.dependencies().size() > 0)
			log.info("Classpath '" + path + "' has dependencies: " + classpath.dependencies());

		return new ClasspathLoader(parent, classpath, basePackage,
				ImmutableList.copyOf(classpath.dependencies()), permissions);
	}


	@Override public synchronized Class<?> loadClass(String name)
		throws ClassNotFoundException
	{
		return loadClass(name, false);
	}

	@Override protected synchronized Class<?> loadClass(String name, boolean resolve)
		throws ClassNotFoundException
	{
		if (getClasspath(name).isEmpty())
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

		final Option<Classpath> classpath = getClasspath(name);
		if (classpath.isEmpty())
			throw new ClassNotFoundException("Unable to load " + name + " from " + classpath);

		final Option<Tuple2<byte[],CodeSource> > bytecode;
		try
		{
			bytecode = AccessController.doPrivileged(
					new PrivilegedExceptionAction<Option<Tuple2<byte[],CodeSource> > >()
				{
					@Override
					public Option<Tuple2<byte[],CodeSource> > run()
						throws ClassNotFoundException, IOException
					{
						return classpath.get().readClass(name);
					}
				});
		}
		catch (PrivilegedActionException e)
		{
			throw new ClassNotFoundException("Unable to load " + name + " from " + classpath, e);
		}

		if (bytecode.isEmpty())
			throw new ClassNotFoundException("Unable to load " + name + " from " + classpath);

		byte[] bytes = bytecode.get()._1;
		CodeSource source = bytecode.get()._2;

		ProtectionDomain domain = new ProtectionDomain(source, permissions);
		Class<?> c = defineClass(name, bytes, 0, bytes.length, domain);
		loadedClasses.put(name, c);
		return c;
	}


	@Override public URL getResource(String name) { return findResource(name); }
	@Override public synchronized URL findResource(String name)
	{
		try
		{
			String externalUrl = base.toExternalForm();

			StringBuilder sb = new StringBuilder();
			sb.append(externalUrl);
			if (!externalUrl.endsWith("/")) sb.append('/');
			sb.append(name);

			return new URL(sb.toString());
		}
		catch(MalformedURLException e) { return null; }
	}


	@Override public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("ClasspathLoader { base url = '");
		sb.append(base.toExternalForm());
		sb.append("', classpaths = '");
		sb.append(dependencies);
		sb.append("', permissions = ");
		sb.append(permissions);
		sb.append(", dependencies = ");
		sb.append(dependencies);
		sb.append("' }");

		return sb.toString();
	}

	/** Get the {@link Classpath} which can serve a given class. */
	private Option<Classpath> getClasspath(String className) throws ClassNotFoundException
	{
		// Can this be served by an existing Classpath?
		for (String packageName : classpaths.keySet())
			if (className.startsWith(packageName))
				return Some.apply(classpaths.get(packageName));

		// Can it be served by a dependency?
		String packageName = className.substring(0, className.lastIndexOf('.'));
		// Try already-loaded classpaths first.
		for (Map.Entry<URL,Option<Classpath> > dep : dependencies.entrySet())
		{
			if (dep.getValue().isEmpty()) continue;

			Classpath cp = dep.getValue().get();
			if (cp.readClass(className).isEmpty()) continue;

			classpaths.put(packageName, cp);
			return Option.apply(cp);
		}

		// If that didn't work, start loading new classpaths.
		synchronized(this)
		{
			for (Map.Entry<URL,Option<Classpath> > dep : dependencies.entrySet())
			{
				if (!dep.getValue().isEmpty()) continue;

				URL url = dep.getKey();
				Option<Classpath> cp = Classpath.open(url, packageName);
				if (cp.isEmpty())
					throw new ClassNotFoundException("Unable to open classpath '" + url + "'");

				dependencies.put(url, cp);

				if (cp.get().readClass(className).isEmpty()) continue;
				else classpaths.put(packageName, cp.get());

				return cp;
			}
		}

		return Option.apply(null);
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
	private ClasspathLoader(ClassLoader parent, Classpath classpath, String basePackage,
		List<URL> dependencies, PermissionCollection permissions)
	{
		super(parent);

		this.loadedClasses = Maps.newHashMap();
		this.dependencies = Maps.newLinkedHashMap();
		for (URL dep : dependencies)
			this.dependencies.put(dep, Option.<Classpath>apply(null));

		this.classpaths = Maps.newLinkedHashMap();
		this.permissions = permissions;

		base = classpath.url();
		classpaths.put(basePackage, classpath);
	}


	/** Base URL for the classpath. */
	private final URL base;

	/** Classes that we've already loaded. */
	private final Map<String, Class<?>> loadedClasses;

	/** External classpaths (which may not have been accessed yet). */
	private final Map<URL, Option<Classpath> > dependencies;

	/** Where we find our classes and resources (package name -> {@link Classpath}). */
	private final Map<String, Classpath> classpaths;

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

	private static final Logger log = Logger.getLogger(ClasspathLoader.class.getCanonicalName());
}

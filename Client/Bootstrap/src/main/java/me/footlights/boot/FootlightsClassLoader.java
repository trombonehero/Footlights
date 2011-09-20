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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;

import com.google.common.collect.Iterables;


/** Loads "core" code (footlights.core.*, footlights.ui.*) from a known source */
public class FootlightsClassLoader extends ClassLoader
{
	/** Constructor */
	public FootlightsClassLoader(Iterable<URL> classpaths)
		throws MalformedURLException
	{
		this.classpaths = Iterables.unmodifiableIterable(classpaths);

		corePermissions = new Permissions();
		corePermissions.add(new AllPermission());
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
		if (!name.startsWith("me.footlights"))
			throw new SecurityException(
				getClass().getCanonicalName() + " can only load core Footlights classes");

		Bytecode bytecode = readBytecode(name);
		ProtectionDomain domain = new ProtectionDomain(bytecode.source, corePermissions);

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



	/** Read bytecode for a core Footlights class. */
	private Bytecode readBytecode(String className)
		throws ClassNotFoundException
	{
		for (URL url : classpaths)
			try
			{
				if (url.toExternalForm().matches(".*\\.jar$"))
					return new JARLoader(url).readBytecode(className);

				else
					return readClassFile(url, className);
			}
			catch(ClassNotFoundException e) {}
			catch(IOException e) {}

		throw new ClassNotFoundException("No " + className + " in " +
				classpaths);
	}



	/** Read bytecode from a class file using a classpath and class name. */
	private Bytecode readClassFile(URL url, String className)
			throws ClassNotFoundException
	{
		// construct the path of the class file
		String path = url.toExternalForm();
		if (!path.startsWith("file:"))
			throw new ClassNotFoundException("The class path URI " + path
				+ " does not start with 'file:'");
		path = path.replaceFirst("file:", "");
		
		String[] subdirs = className.split("\\.");
		for (int i = 0; i < (subdirs.length - 1); i++)
			path = path + File.separatorChar + subdirs[i];


		// open the file and read it
		String classFileName = subdirs[subdirs.length - 1] + ".class";
		File file = new File(path + File.separatorChar + classFileName);

		try
		{
			FileInputStream in = new FileInputStream(file);
			byte[] raw = new byte[(int) file.length()];

			for (int offset = 0; offset < raw.length; )
				offset += in.read(raw, offset, raw.length - offset);

			in.close();

			Bytecode bytecode = new Bytecode();
			bytecode.raw = raw;
			bytecode.source = new CodeSource(url, new CodeSigner[0]);
			return bytecode;
		}
		catch(IOException e)
		{
			throw new ClassNotFoundException(e.getLocalizedMessage());
		}
	}


	/** Cached permissions given to core classes. */
	private final Permissions corePermissions;

	/** Where we can find core classes. */
	private final Iterable<URL> classpaths;
}

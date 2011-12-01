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
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Policy;
import java.security.Security;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.common.collect.Sets;


/**
 * Bootstraps the Footlights system.
 *
 * This might seem silly, but we need this bootstrapper so that we can create
 * our own ClassLoader with full privilege and guarantee that no other classes
 * will be linked with such privilege.
 */
class Bootstrapper
{
	private static class UI
	{
		public UI(String name, String sourceDirectory, String packageName, String className)
		{
			this.name = name;
			this.sourceDirectory = sourceDirectory;
			this.packageName = packageName;
			this.className = className;
		}

		final String name;
		final String sourceDirectory;
		final String packageName;
		final String className;
	}

	public static void main(String[] args) throws Exception
	{
		String bootPath = "Bootstrap";
		List<String> corePaths = new ArrayList<String>();
		corePaths.add("Core");

		final UI[] uis =
		{
			new UI("Local Web UI", "UI/Web", "me.footlights.ui.web", "WebUI"),
//			new UI("Local Swing UI", "UI/Swing", "me.footlights.ui.swing", "SwingUI"),
		};

		for (UI ui : uis) corePaths.add(ui.sourceDirectory);

		LinkedHashSet<URL> coreClasspaths =
			expandCorePath(System.getProperty("java.class.path"),
					bootPath, corePaths);

		FootlightsClassLoader classLoader = new FootlightsClassLoader(coreClasspaths);

		// Install crypto provider.
		Security.addProvider(new BouncyCastleProvider());

		// Set up our security policy and start enforcing it.
		Policy.setPolicy(new RestrictivePolicy());
		System.setSecurityManager(new SecurityManager());

		// Ensure that Bootstrapper, as the most privileged code around, can still do anything.
		AccessController.checkPermission(new AllPermission());

		// Load the Footlights class.
		Class<?> footlightsClass = classLoader.loadClass("me.footlights.core.Footlights");
		Class<?> coreClass = classLoader.loadClass("me.footlights.core.Kernel");

		Object footlights = coreClass
			.getMethod("init", ClassLoader.class)
			.invoke(null,  classLoader);

		// Load the UI(s).
		List<Thread> uiThreads = new ArrayList<Thread>();
		for (final UI ui : uis)
		{
			final String className = ui.packageName + "." + ui.className;
			log.info(format("Loading UI '%s' (%s/%s)", ui.name, ui.sourceDirectory, className));

			Class<?> uiClass = classLoader.loadClass(className);
			Method init = uiClass.getMethod("init", footlightsClass);
			Object uiObject = init.invoke(null, footlights);

			uiThreads.add(new Thread((Runnable) uiObject, ui.name));
		}

		for (Thread thread : uiThreads) thread.start();

		// wait for the UI(s) to exit
		for (Thread thread : uiThreads) 
		{
			log.info("Waiting for " + thread.getName() + " to end");
			thread.join();
		}

		System.exit(0);
	}

	private static LinkedHashSet<URL> expandCorePath(final String bootClasspath,
			final String bootDirectory, final Iterable<String> coreDirectories)
		throws FileNotFoundException, MalformedURLException
	{
		LinkedHashSet<URL> classpaths = Sets.newLinkedHashSet();

		for (final String bootPath : bootClasspath.split(":"))
			for (String coreDirectory : coreDirectories)
			{
				String path = bootPath.replace(bootDirectory, coreDirectory);
				if (!new File(path).exists()) continue;

				if (path.startsWith("/")) path = "file:" + path;
				URL url = new URL(path);

				if (classpaths.contains(url)) continue;
				else classpaths.add(url);
			}

		return classpaths;
	}


	/** Log. */
	private static final Logger log = Logger.getLogger(Bootstrapper.class.getName());
}

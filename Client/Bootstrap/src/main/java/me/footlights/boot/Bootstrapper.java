package me.footlights.boot;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Bootstraps the Footlights system.
 *
 * This might seem silly, but we need this bootstrapper so that we can create
 * our own ClassLoader with full privilege and guarantee that no other classes
 * will be linked with such privilege.
 */
public class Bootstrapper
{
	private static class UI
	{
		public UI(String name, String sourceDirectory,
		          String packageName, String className)
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

		List<URL> coreClasspaths =
			expandCorePath(System.getProperty("java.class.path"),
					bootPath, corePaths);

		FootlightsClassLoader classLoader =
			new FootlightsClassLoader(coreClasspaths);


		// Load the Footlights class.
		Class<?> footlightsClass =
			classLoader.loadClass("me.footlights.core.Core");

		Object footlights = footlightsClass.newInstance();


		// Load the UI(s).
		List<Thread> uiThreads = new ArrayList<Thread>();
		for (final UI ui : uis)
		{
			final String className = ui.packageName + "." + ui.className;

			Class<?> uiClass = classLoader.loadClass(className);
			Constructor<?> constructor = uiClass.getConstructors()[0];
			Object obj = constructor.newInstance(new Object[] { footlights });
			uiThreads.add(new Thread((Runnable) obj, ui.name));
		}

		for (Thread thread : uiThreads) thread.start();


		// now set up our security policy and start enforcing it
		Policy.setPolicy(new RestrictivePolicy());
		System.setSecurityManager(new SecurityManager());


		// wait for the UI(s) to exit
		for (Thread thread : uiThreads) 
		{
			System.out.println("Waiting for " + thread.getName() + " to end");
			thread.join();
		}

		System.exit(0);
	}

	private static List<URL> expandCorePath(final String bootClasspath,
			final String bootDirectory, final Iterable<String> coreDirectories)
		throws FileNotFoundException, MalformedURLException
	{
		List<URL> classpaths = new ArrayList<URL>();

		for (String coreDirectory : coreDirectories)
		{
			String path = bootClasspath.replace(bootDirectory, coreDirectory);
			if (path.startsWith("/"))
			{
				if (!new File(path).exists())
					throw new FileNotFoundException(path);

				path = "file:" + path;
			}
			classpaths.add(new URL(path));
		}

		return classpaths;
	}
}

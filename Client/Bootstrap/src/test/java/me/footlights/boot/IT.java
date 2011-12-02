package me.footlights.boot;

import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

import me.footlights.core.Kernel;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;


/** Integration tests for bootstrap class loading. */
public class IT
{
	public IT()
	{
		coreClasspaths = Lists.newArrayList();
		for (String path : System.getProperty("java.class.path").split(":"))
			if (path.contains("Bootstrap") || path.contains("Core"))
				coreClasspaths.add(path);
	}


	@Before public void setUp() throws Exception
	{
		LinkedHashSet<URL> classpaths = Sets.newLinkedHashSet();
		for (String path : System.getProperty("java.class.path").split(":"))
		{
			path = path.replace("Client/Bootstrap", "Client/Core");

			if (path.startsWith("/")) path = "file:" + path;
			classpaths.add(new URL(path));
		}

		loader = new FootlightsClassLoader(classpaths);
		logger = Mockito.mock(java.util.logging.Logger.class);
	}

	/** Make sure we can load classes from {@link me.footlights.core}. */
	@Test public void testLoadingCore() throws ClassNotFoundException
	{
		Class<?> c = loader.loadClass("me.footlights.core.Core");
		assertNotNull(c);
		assertEquals("me.footlights.core.Core", c.getCanonicalName());

		PermissionCollection permissions = c.getProtectionDomain().getPermissions();
		assertTrue(permissions.implies(new AllPermission()));
	}

	@Test public void testGoodPlugin() throws Exception
	{

		Class<?> c = loader.loadClass(GOOD_PLUGIN);

		assertNotNull(c);
		assertEquals(GOOD_CLASSNAME, c.getCanonicalName());

		PermissionCollection permissions = c.getProtectionDomain().getPermissions();
		assertFalse(c.getName() + " should not get AllPermission",
			permissions.implies(new AllPermission()));

		assertFalse(c.getName() + " should not get 'exitVM' permission",
			permissions.implies(new RuntimePermission("exitVM")));

		for (String path : coreClasspaths)
			assertFalse(c.getName() + " should not be able to read " + path,
				permissions.implies(new FilePermission(path, "read")));


		Method init = null;
		for (Method m : c.getMethods())
		{
			if (Modifier.isStatic(m.getModifiers())
				&& (m.getName() == "init")
				&& (m.getParameterTypes().length == 2))
			{
				init = m;
				break;
			}
		}

		if (init == null)
			fail("Unable to find static init() method in plugin " + c.getCanonicalName());

		Object plugin = init.invoke(null, null, logger);
		assertNotNull(plugin);

		verify(logger, atLeastOnce()).info(anyString());
	}

	/** Make sure that we can load a plugin and then re-load it. */
	@Test public void testReload() throws Exception
	{
		Class<?> c1 = loader.loadClass(GOOD_PLUGIN);
		Class<?> c2 = loader.loadClass(GOOD_PLUGIN);
		assertNotSame(c1.getClassLoader(), c2.getClassLoader());
	}

	/** Make sure that we can load resource from a JAR file. */
	@Test public void testResources() throws Exception
	{
		Class<?> c = loader.loadClass(GOOD_PLUGIN);
		ClassLoader loader = c.getClassLoader();

		// Read the .class file for the plugin's entry point.
		URL classFileUrl = loader.getResource(GOOD_CLASS_FILE);
		InputStream in = classFileUrl.openStream();
		assertNotNull(in);

		byte buffer[] = new byte[4096];
		int bytes = in.read(buffer);

		// Just make sure the file is a valid Java class file (magic 0xCAFEBABE).
		byte magic[] = { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };
		assertTrue(bytes > magic.length);
		for (int i = 0; i < magic.length; i++) assertEquals(magic[i], buffer[i]);
	}

	/** Make sure that we can only load our own resources. */
	@Test public void testLoadUnauthorizedResources() throws Exception
	{
		loader.loadClass(GOOD_PLUGIN);
		Class<?> wicked = loader.loadClass(WICKED_PLUGIN);

		ClassLoader wickedLoader = wicked.getClassLoader();

		// Try to load code from the "good" plugin.
		URL url = wickedLoader.getResource(GOOD_CLASS_FILE);
		try
		{
			url.openStream();
			fail("Should not be able to open " + GOOD_CLASS_FILE + " via wicked plugin");
		}
		catch (FileNotFoundException e) { /* expected result */ }

		// Try to load core Footlights code.
		String className = Kernel.class.getCanonicalName();
		url = wickedLoader.getResource(className);
		try
		{
			url.openStream();
			fail("Should not be able to open " + className + " via wicked plugin");
		}
		catch (FileNotFoundException e) { /* expected result */ }
	}


	/** Build a path to a plugin JAR, based on the current classpath. */
	private static String pluginUri(String projectDir, String projectName, String pluginClassName)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("jar:file://");
		sb.append(
			System.getProperty("java.class.path")
				.replaceFirst("Client/Bootstrap/.*", "Client/Plugins/"));
		sb.append(projectDir);
		sb.append("/target/");
		sb.append(projectName);
		sb.append("-HEAD.jar!/");
		sb.append(pluginClassName);

		return sb.toString();
	}

	private static final String GOOD_CLASSNAME = "me.footlights.demo.plugins.good.GoodPlugin";
	private static final String GOOD_CLASS_FILE = GOOD_CLASSNAME.replaceAll("\\.", "/") + ".class";
	private static final String GOOD_PLUGIN = pluginUri("Good", "good-plugin", GOOD_CLASSNAME);

	private static final String WICKED_CLASSNAME = "me.footlights.demo.plugins.wicked.WickedPlugin";
	private static final String WICKED_PLUGIN =
		pluginUri("Wicked", "wicked-plugin", WICKED_CLASSNAME);

	private FootlightsClassLoader loader;
	private Logger logger;
	private final List<String> coreClasspaths;
}

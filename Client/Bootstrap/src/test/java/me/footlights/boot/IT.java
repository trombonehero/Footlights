package me.footlights.boot;

import java.io.FilePermission;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

import me.footlights.plugin.KernelInterface;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

		kernelInterface = Mockito.mock(KernelInterface.class);
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
		final String className = "me.footlights.demo.plugins.good.GoodPlugin";

		Class<?> c = loader.loadClass(pluginUri("Good", "good-plugin", className));

		assertNotNull(c);
		assertEquals(className, c.getCanonicalName());

		PermissionCollection permissions = c.getProtectionDomain().getPermissions();
		assertFalse(className + " should not get AllPermission",
			permissions.implies(new AllPermission()));

		assertFalse(className + " should not get 'exitVM' permission",
			permissions.implies(new RuntimePermission("exitVM")));

		for (String path : coreClasspaths)
			assertFalse(className + " should not be able to read " + path,
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

	private FootlightsClassLoader loader;
	private Object kernelInterface;
	private Logger logger;
	private final List<String> coreClasspaths;
}

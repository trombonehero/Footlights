package me.footlights.boot;

import java.io.FilePermission;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import static org.mockito.Mockito.when;


/** Tests {@link PluginClassLoader}. */
public class PluginClassLoaderITCase
{
	public PluginClassLoaderITCase()
	{
		coreClasspaths = Lists.newArrayList();
		for (String path : System.getProperty("java.class.path").split(":"))
			if (path.contains("Bootstrap") || path.contains("Core"))
				coreClasspaths.add(path);
	}

	@Before public void setUp()
	{
		coreLoader = Mockito.mock(FootlightsClassLoader.class);
		loader = new PluginClassLoader(coreLoader);
	}


	/** Make sure we delegate loading core Footlights classes. */
	@SuppressWarnings("unchecked")
	@Test public void testLoadingCore() throws ClassNotFoundException
	{
		@SuppressWarnings("rawtypes")
		Class mockCoreClass = Bytecode.class;
		final String className = "me.footlights.Foo";

		when(coreLoader.findClass(Mockito.eq(className))).thenReturn(mockCoreClass);

		Class<?> plugin = loader.loadClass(className);
		assertEquals(mockCoreClass, plugin);
	}

	@Test public void testGoodPlugin() throws ClassNotFoundException
	{
		final String className = "me.footlights.demo.plugins.good.GoodPlugin";

		Class<?> c = loader.loadClass(pluginUri("Good", "good-plugin", className));

		assertNotNull(c);
		assertEquals(className, c.getCanonicalName());

		PermissionCollection permissions = c.getProtectionDomain().getPermissions();
		assertFalse(permissions.implies(new AllPermission()));
		assertFalse(permissions.implies(new RuntimePermission("exitVM")));
		for (String path : coreClasspaths)
			assertFalse(className + " should not be able to read " + path,
				permissions.implies(new FilePermission(path, "read")));
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

	private FootlightsClassLoader coreLoader;
	private PluginClassLoader loader;

	private final List<String> coreClasspaths;
}

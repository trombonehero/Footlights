package me.footlights.boot;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.mockito.Mockito.when;


/** Tests {@link PluginClassLoader}. */
public class PluginClassLoaderTest
{
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
}

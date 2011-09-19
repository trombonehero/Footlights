package me.footlights.boot;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/** Tests {@link FootlightsClassLoader}. */
public class FootlightsClassLoaderTest
{
	@Before public void setUp() throws MalformedURLException
	{
		pluginLoader = Mockito.mock(PluginClassLoader.class);

		LinkedHashSet<URL> classpaths = Sets.newLinkedHashSet();
		for (String path : System.getProperty("java.class.path").split(":"))
		{
			path = path.replace("Client/Bootstrap", "Client/Core");

			if (path.startsWith("/")) path = "file://" + path;
			classpaths.add(new URL(path));
		}

		loader = new FootlightsClassLoader(classpaths);
	}

	/** Make sure we can load classes from {@link me.footlights.core}. */
	@Test public void testLoadingCore() throws ClassNotFoundException
	{
		Class<?> c = loader.loadClass("me.footlights.core.Core");
		assertNotNull(c);
		assertEquals("me.footlights.core.Core", c.getCanonicalName());
	}


	private PluginClassLoader pluginLoader;
	private FootlightsClassLoader loader;

	private static final String PLUGIN =
    	"jar:file://"
    	 + System.getProperty("java.class.path")
    	   .replaceFirst("Client/PluginIntegrationTest/.*", "Client/Plugins/PLUGIN_NAME/target/");
}

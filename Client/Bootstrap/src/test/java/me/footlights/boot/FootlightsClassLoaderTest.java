package me.footlights.boot;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/** Tests {@link FootlightsClassLoader}. */
public class FootlightsClassLoaderTest
{
	@Before public void setUp() throws MalformedURLException
	{
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


	private FootlightsClassLoader loader;
}

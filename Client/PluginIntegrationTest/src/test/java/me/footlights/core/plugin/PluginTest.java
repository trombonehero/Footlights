package me.footlights.core.plugin;

import java.net.URI;

import me.footlights.core.plugin.PluginLoader;
import me.footlights.core.plugin.PluginWrapper;

import org.junit.Before;
import org.junit.Test;


public class PluginTest
{
	@Before public void setUp()
	{
		footlights = new me.footlights.core.Core();
		loader = new PluginLoader(footlights);
	}

	@Test public void testTrivialPlugin() throws Throwable
	{
		PluginWrapper plugin = loader.loadPlugin(
				"Trivial Demo Plugin", new URI("me.footlights.core.plugin.TrivialPlugin"));
		plugin.run();

		// TODO
//		assertEquals(TrivialPlugin.OUTPUT, plugin.output());
	}


	/** Load the "good" (non-malicious) test plugin */
	/*
	@Test public void testGoodPlugin() throws Throwable
	{
		PluginWrapper p = run(
				PLUGIN.replace("PLUGIN_NAME", "Good")
				+ "good.jar!/me.footlights.demo.plugins.good.GoodPlugin");

		p.output();
	}
	*/


	/** Load the "wicked" test plugin */
	/*
	@Test public void loadWickedPlugin() throws PluginLoadException
	{
		PluginWrapper p =
			run(PLUGIN + "wicked.jar!/footlights.demo.plugins.wicked.WickedPlugin");

		try
		{
			p.output();
			fail("Plugin should've failed with a security exception");
		}
		catch(SecurityException e)
		{
			// there's not much we can do to automatically test security
			// (the bootstrap trickery wouldn't work with JUnit)
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			fail("Plugin should've failed with a security exception, not " + t);
		}
	}
	*/


	private me.footlights.core.Core footlights;
	private PluginLoader loader;
/*
	private static final String PLUGIN =
		"jar:file://"
		 + System.getProperty("java.class.path")
		   .replaceFirst("Client/Core/.*", "Client/Plugins/PLUGIN_NAME/target/");
*/
}

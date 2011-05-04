package me.footlights.core.plugin;

import me.footlights.core.plugin.PluginLoadException;
import me.footlights.core.plugin.PluginLoader;
import me.footlights.core.plugin.PluginWrapper;

import org.junit.Test;
import static org.junit.Assert.*;


public class PluginTests
{
	protected PluginWrapper run(String url) throws PluginLoadException
	{
		me.footlights.core.Core core = new me.footlights.core.Core();

		PluginWrapper plugin = new PluginLoader(core).loadPlugin(url);
		plugin.run();

		return plugin;
	}


	/** Load the "good" (non-malicious) test plugin */
	@Test public void testGoodPlugin() throws PluginLoadException
	{
		PluginWrapper p =
			run(PLUGIN + "good.jar!/me.footlights.demo.plugins.good.GoodPlugin");

		try { p.output(); }
		catch(Throwable t)
		{
			t.printStackTrace();
			fail("The \"good\" plugin threw: " + t);
		}
	}


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
	


	final String BASE_URL =
		"file://"
		+ System.getProperty("java.class.path").replaceFirst("Client/.*", "Client/JARs");

	final String PLUGIN = "jar:" + BASE_URL + "/Plugins/";
}


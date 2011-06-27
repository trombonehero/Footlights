package me.footlights.core.plugin;

import java.net.URI;
import java.util.logging.Logger;

import me.footlights.core.plugin.PluginLoader;
import me.footlights.core.plugin.PluginWrapper;
import me.footlights.plugin.KernelInterface;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;


/** Test loading {@link Plugin}s. */
public class PluginTest
{
	@Before public void setUp()
	{
		mockKernel = Mockito.mock(KernelInterface.class);
		mockLog = Mockito.mock(Logger.class);

		loader = new PluginLoader(mockKernel);
	}


	@Test public void testTrivialPlugin() throws Throwable
	{
		PluginWrapper plugin = loader.loadPlugin(
				"Trivial Demo Plugin",
				new URI("me.footlights.core.plugin.TrivialPlugin"),
				mockLog
			);

		plugin.run();

		Mockito.verify(mockLog).info(TrivialPlugin.OUTPUT);
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


	private KernelInterface mockKernel;
	private Logger mockLog;

	private PluginLoader loader;
/*
	private static final String PLUGIN =
		"jar:file://"
		 + System.getProperty("java.class.path")
		   .replaceFirst("Client/Core/.*", "Client/Plugins/PLUGIN_NAME/target/");
*/
}

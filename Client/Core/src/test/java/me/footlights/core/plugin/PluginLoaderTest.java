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
public class PluginLoaderTest
{
	@Before public void setUp()
	{
		mockLog = Mockito.mock(Logger.class);
		loader = new PluginLoader();
	}


	@Test public void testTrivialPlugin() throws Throwable
	{
		PluginWrapper plugin = loader.loadPlugin(
				"Trivial Demo Plugin",
				new URI("me.footlights.core.plugin.TrivialPlugin"),
				mockLog
			);

		plugin.run(Mockito.mock(KernelInterface.class));

		Mockito.verify(mockLog).info(TrivialPlugin.OUTPUT);
	}


	private Logger mockLog;
	private PluginLoader loader;
}

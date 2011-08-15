/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.footlights.core.plugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import me.footlights.plugin.KernelInterface;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;


/** Integration tests for plugin loading (actually loads and verifies a signed JAR file). */
public class PluginIT
{
	@Before public void setUp()
	{
		mockKernel = Mockito.mock(KernelInterface.class);
		mockLog = Mockito.mock(Logger.class);

		loader = new PluginLoader();
	}

	
	/** Load the "good" (non-malicious) test plugin */
	@Test public void testGoodPlugin() throws Throwable
	{
		PluginWrapper plugin = loader.loadPlugin(
				"Good Plugin",
				new URI(PLUGIN.replace("PLUGIN_NAME", "Good")
					+ "good.jar!/me.footlights.demo.plugins.good.GoodPlugin"),
				mockLog);

		plugin.run(mockKernel);

		Mockito.verify(mockLog).info("foo");
	}


	/** Load and run the "wicked" test plugin, ensuring that it is properly confined. */
	@Test public void loadWickedPlugin() throws PluginLoadException, URISyntaxException
	{
		PluginWrapper plugin = loader.loadPlugin(
				"Wicked Plugin",
				new URI(PLUGIN.replace("PLUGIN_NAME", "Good")
					+ "good.jar!/me.footlights.demo.plugins.wicked.WickedPlugin"),
				mockLog);

		try
		{
			plugin.run(mockKernel);
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


	private static final String PLUGIN =
		"jar:file://"
		 + System.getProperty("java.class.path")
		   .replaceFirst("Client/Core/.*", "Client/Plugins/PLUGIN_NAME/target/");

	private KernelInterface mockKernel;
	private Logger mockLog;

	private PluginLoader loader;
}

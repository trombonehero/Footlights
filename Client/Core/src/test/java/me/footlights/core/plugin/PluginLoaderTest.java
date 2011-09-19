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
import java.util.logging.Logger;

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
		loader = new PluginLoader(null);
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

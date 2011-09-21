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
package me.footlights.ui.web;

import me.footlights.core.Footlights;
import me.footlights.plugin.AjaxHandler;
import me.footlights.plugin.JavaScript;
import me.footlights.plugin.WebRequest;


/** The global context - code sent here has full DOM access. */
class GlobalContext extends Context
{
	GlobalContext(final Footlights footlights, final AjaxServer server)
	{
		register("initialize", new Initializer());
		register("load_plugin", new PluginLoader(footlights, server));
		register("reset", new AjaxHandler() {
			@Override
			public JavaScript service(WebRequest request)
			{
				while (footlights.plugins().size() > 0)
					footlights.unloadPlugin(
						footlights.plugins().iterator().next());

				server.init();
				return new JavaScript().append("window.location.reload()");
			}
		});

		register("cajole", new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request) throws Throwable
				{
					JavaScript code = new JavaScript();
					code.append(
						"var sandbox = sandboxes.getOrCreate('sandbox', rootContext, rootContext.log, 0, 0, 200, 200);");

					try
					{
						server.register("sandbox", new TestContext());
						code.append("sandbox.load('test.js');");
					}
					catch (Exception e)
					{
						code.append("sandbox.log('Error registering context: " + e + "')");
					}
					return code;
				}
			});
	}
}

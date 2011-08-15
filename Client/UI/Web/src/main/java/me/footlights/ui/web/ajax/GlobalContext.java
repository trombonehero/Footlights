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
package me.footlights.ui.web.ajax;

import me.footlights.core.Footlights;
import me.footlights.ui.web.Request;


/** The global context - code sent here has full DOM access. */
class GlobalContext extends Context
{
	GlobalContext(final Footlights footlights)
	{
		super("global");

		register("initialize", new Initializer());
		register("load_plugin", new PluginLoader(footlights));
		register("echo", new AjaxHandler() {
			@Override
			public JavaScript service(Request request)
			{
				return
					new JavaScript()
						.append("showAjaxResponse('echo', '")
						.append(request.path().replaceFirst("^/?echo%20", ""))
						.append("');");
			}
		});
		register("reset", new AjaxHandler() {
			@Override
			public JavaScript service(Request request)
			{
				while(footlights.plugins().size() > 0)
					footlights.unloadPlugin(
						footlights.plugins().iterator().next());

				return new JavaScript().append("window.location.reload()");
			}
		});
		
		
		/*
			if(request.path().equals("/cajole"))
			{
				type = "code";
				context = "to-be-cajoled";

				InputStream in = getClass().getResourceAsStream("content/sandbox.js");

				final char[] buffer = new char[2048];
				Reader reader = new InputStreamReader(in);
				int bytes;
				do {
				  bytes = reader.read(buffer, 0, buffer.length);
				  if (bytes > 0) content.append(buffer, 0, bytes);
				} while (bytes >= 0);

				content.append("");
				/*
				content.append("var sandbox = document.createElement('div');");
				content.append("sandbox.className = 'sandbox';");
				content.append("sandbox.width = 400;");
				content.append("document.getElementById('content').appendChild(sandbox);");
				content.append("retrieveAndRunModule('sandbox.js', 'sandboxed', sandbox, log);");
				content.append("};");
		 */
	}
}

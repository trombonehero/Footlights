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

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import me.footlights.core.*;
import me.footlights.plugin.JavaScript;
import me.footlights.plugin.WebRequest;


/** Acts as an Ajax server for the JavaScript client */
public class AjaxServer implements WebServer
{
	static final String DEFAULT_CONTEXT = "unspecified context";


	public AjaxServer(Footlights footlights)
	{
		contexts = Maps.newLinkedHashMap();

		pluginContext = new Context();
		contexts.put("plugin", pluginContext);

		contexts.put("global",
			new GlobalContext(footlights, this, new PluginLoader(footlights, pluginContext)));
	}

	@Override public String name() { return "Ajax"; }
	@Override public Response handle(WebRequest request) throws Throwable
	{
		String contextName = request.prefix();
		Context context = contexts.get(contextName);
		if (context == null)
			throw new IllegalArgumentException("No such context '" + contextName + "'");

		log.fine("Routing request to " + context);
		JavaScript response = context.service(request.shift());

		return Response.newBuilder()
			.setResponse("text/javascript",
				new ByteArrayInputStream(response.exec().getBytes()))
			.build();
	}


	synchronized void reset() { pluginContext.unloadHandlers(); }

	synchronized void register(String name, Context context)
	{
		if (contexts.containsKey(name))
			throw new RuntimeException(name + " already registered");

		contexts.put(name, context);
	}


	/** Log. */
	private static final Logger log = Logger.getLogger(AjaxServer.class.getName());

	/** Context for the Ajax handlers of {@link Plugin}s. */
	private final Context pluginContext;

	/** Plugin/sandbox contexts. */
	private final LinkedHashMap<String, Context> contexts;
}

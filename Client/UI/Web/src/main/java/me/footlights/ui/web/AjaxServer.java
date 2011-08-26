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
import java.net.URI;
import java.util.LinkedHashMap;

import me.footlights.core.*;
import me.footlights.core.plugin.PluginWrapper;

import static me.footlights.core.Log.log;


/** Acts as an Ajax server for the JavaScript client */
public class AjaxServer implements WebServer
{
	static final String DEFAULT_CONTEXT = "unspecified context";


	public AjaxServer(Footlights footlights)
	{
		this.footlights = footlights;
		this.contexts = new LinkedHashMap<String, Context>();

		// TODO(jon): do this registration somewhere else
		register(new GlobalContext(footlights));
		register(new Context("foo").register("hello", new HelloWorldPlugin()));
		register(new Context("echo").register("echo", new EchoPlugin()));
		register(new Context("sandbox").register("foo", new TestHandler()));
	}

	@Override public String name() { return "Ajax"; }
	@Override public Response handle(Request request)
	{
		Context context = contexts.get(request.prefix());
		if (context == null)
			throw new IllegalArgumentException("No such context '" + context + "'");

		log("Routing request to " + context);

		AjaxResponse response = context.service(request.shift());

		return Response.newBuilder()
			.setResponse("text/xml",
				new ByteArrayInputStream(response.toXML().getBytes()))
			.build();
	}


	synchronized void register(Context context)
	{
		if (contexts.containsKey(context.name))
			throw new RuntimeException(context + " already registered");

		contexts.put(context.name, context);
	}


	private String runPlugin(URI url) throws Throwable
	{
		PluginWrapper plugin = footlights.loadPlugin("foo", url);
		plugin.run(footlights);

		String result = "(loaded: '" + plugin.getPluginName() + "')\n";

		return result;
	}


	/** Loads plugins */
	private final Footlights footlights;

	/** Plugin/sandbox contexts. */
	private final LinkedHashMap<String, Context> contexts;
}

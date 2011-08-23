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

import java.io.*;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.regex.*;

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
	}

	@Override public String name() { return "Ajax"; }
	@Override public String mimeType(String request) { return "text/xml"; }
	@Override public Response handle(Request request)
	{
		{
			Context context = getContext(request);
			if (context != null)
			{
				log("Allowing " + context + " to handle request");
				return Response.newBuilder()
					.setResponse("text/xml",
						new ByteArrayInputStream(context.service(request).toXML().getBytes()))
					.build();
			}
		}

		String type = "";
		String context = DEFAULT_CONTEXT;
		StringBuilder content = new StringBuilder();

		log("Processing Ajax command...");

		try
		{
			if(Pattern.matches("/run_good.*", request.path()))
			{
				type = "code";

				String output = runPlugin(new URI(GOOD_PLUGIN));
				output = output.replace("'", "\\'");
				output = output.replace("\n", "\\n");

				content.append("showAjaxResponse('plugin output', '" + output + "')");
			}
			else if(Pattern.matches("/run_evil.*", request.path()))
			{
				type = "code";

				String output = runPlugin(new URI(WICKED_PLUGIN));
				output = output.replace("'", "\\'");
				output = output.replace("\n", "\\n");

				content.append("showAjaxResponse('plugin output', '" + output + "');");
			}
			else if(request.path().equals("/shutdown"))
			{
				log("shutdown");

				type = "shutdown";
				content.append("Client shutting down...");
			}
			else
			{
				type = "error";
				content.append("unknown Ajax request '" + request + "'");
			}
		}
		catch(Throwable e)
		{
			Log.log("Error serving URI '" + request + "':");
			e.printStackTrace(Log.instance().stream());

			type = "error";

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintWriter writer = new PrintWriter(baos);

			e.printStackTrace(writer);
			writer.flush();

			content.append(baos.toString());
		}

		String contentString = content.toString();
		contentString = contentString.replace("<", "%3C");
		contentString = contentString.replace(">", "%3E");

		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<response>\n");
		sb.append("\t<type>" + type + "</type>\n");
		sb.append("\t<context>" + context + "</context>\n");
		sb.append("\t<content>" + content + "</content>\n");
		sb.append("</response>");

		return Response.newBuilder()
			.setResponse("text/xml", new ByteArrayInputStream(sb.toString().getBytes()))
			.build();
	}


	synchronized void register(Context context)
	{
		if (contexts.containsKey(context.name))
			throw new RuntimeException(context + " already registered");

		contexts.put(context.name, context);
	}
	
	private Context getContext(Request request)
	{
		String contextName = null;

		int slash = request.path().indexOf("/", 1);
		if (slash > 0)
			contextName = request.path().substring(1, slash);

		if (contextName == null)
		{
			if (contexts.entrySet().isEmpty()) return null;
			return contexts.entrySet().iterator().next().getValue();
		}
		else return contexts.get(contextName);
	}

	protected String runPlugin(URI url) throws Throwable
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


	/** Plugin URIs */
	public static final String PLUGIN = "jar:" + Constants.PLUGIN_URL;

	public static final String GOOD_PLUGIN =
		"/good.jar!/footlights.demo.plugins.good.GoodPlugin";

	public static final String WICKED_PLUGIN =
		"/wicked.jar!/footlights.demo.plugins.wicked.WickedPlugin";
}

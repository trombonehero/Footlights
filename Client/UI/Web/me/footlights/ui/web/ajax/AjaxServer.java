package me.footlights.ui.web.ajax;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.regex.*;

import me.footlights.core.*;
import me.footlights.core.plugin.PluginWrapper;
import me.footlights.ui.web.Constants;
import me.footlights.ui.web.Request;
import me.footlights.ui.web.WebServer;

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
		register(new Context("bar").register("hello", new HelloWorldPlugin()));
	}

	@Override public String name() { return "Ajax"; }
	@Override public String mimeType(String request) { return "text/xml"; }
	@Override public InputStream handle(Request request)
	{
		{
			Context context = getContext(request);
			if (context != null)
				return new ByteArrayInputStream(
						context.service(request).toXML().getBytes());
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

				String output = runPlugin(GOOD_PLUGIN);
				output = output.replace("'", "\\'");
				output = output.replace("\n", "\\n");

				content.append("showAjaxResponse('plugin output', '" + output + "')");
			}
			else if(Pattern.matches("/run_evil.*", request.path()))
			{
				type = "code";

				String output = runPlugin(WICKED_PLUGIN);
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
			Log.log("Error serving URL '" + request + "':");
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

		return new ByteArrayInputStream(sb.toString().getBytes());
	}


	synchronized void register(Context context)
	{
		if (contexts.containsKey(context.name))
			throw new RuntimeException(context + " already registered");

		contexts.put(context.name, context);
	}
	
	private Context getContext(Request request)
	{
		String contextName = request.query().get("context");
		if (contextName == null)
		{
			if (contexts.entrySet().isEmpty()) return null;
			return contexts.entrySet().iterator().next().getValue();
		}
		else return contexts.get(contextName);
	}

	protected String runPlugin(String url) throws Throwable
	{
		PluginWrapper plugin = footlights.loadPlugin(url);
		plugin.run();

		String result = "(plugin loaded at " + plugin.wrapped().loaded() + ")\n";
		result += plugin.output();

		return result;
	}


	/** Loads plugins */
	private final Footlights footlights;

	/** Plugin/sandbox contexts. */
	private final LinkedHashMap<String, Context> contexts;


	/** Plugin URLs */
	public static final String PLUGIN = "jar:" + Constants.PLUGIN_URL;

	public static final String GOOD_PLUGIN
		= PLUGIN + "/good.jar!/footlights.demo.plugins.good.GoodPlugin";

	public static final String WICKED_PLUGIN
		= PLUGIN + "/wicked.jar!/footlights.demo.plugins.wicked.WickedPlugin";
}

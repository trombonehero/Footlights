package me.footlights.ui.web.ajax;

import java.net.URI;

import me.footlights.core.Footlights;
import me.footlights.core.plugin.PluginWrapper;
import me.footlights.ui.web.Constants;
import me.footlights.ui.web.Request;


public class PluginLoader implements AjaxHandler
{
	PluginLoader(Footlights footlights)
	{
		this.footlights = footlights;
	}

	@Override
	public JavaScript service(Request request) throws Throwable
	{
		String url = request.path().replaceFirst("^/?load_plugin%20", "");
		String name = url;

		if (url.startsWith("/")) url = PLUGIN_BASE + url;

		PluginWrapper plugin = footlights.loadPlugin(name, new URI(url));
		plugin.run(footlights);

		JavaScript response = new JavaScript();
		response.append("console.log('\"");
		response.append(plugin.getPluginName());
		response.append("\" loaded');");

		return response;
	}



	/** Plugin URIs */
	private static final String PLUGIN_BASE = "jar:" + Constants.PLUGIN_URL;

	private final Footlights footlights;
}

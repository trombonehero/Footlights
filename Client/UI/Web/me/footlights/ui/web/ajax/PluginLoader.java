package me.footlights.ui.web.ajax;

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
		if (url.startsWith("/")) url = PLUGIN_BASE + url;

		PluginWrapper plugin = footlights.loadPlugin(url);
		plugin.run();

		JavaScript response = new JavaScript();
		response.append("console.log('\"");
		response.append(plugin.getName());
		response.append("\" loaded at ");
		response.append(plugin.wrapped().loaded().toString());
		response.append("');");
		response.append("showAjaxResponse('plugin output', '");
		response.append(plugin.output().replace("'", "\\'").replace("\n", "\\n"));
		response.append("');");

		return response;
	}



	/** Plugin URLs */
	private static final String PLUGIN_BASE = "jar:" + Constants.PLUGIN_URL;

	private final Footlights footlights;
}

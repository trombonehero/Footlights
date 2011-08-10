package me.footlights.ui.web;

import me.footlights.core.Footlights;
import me.footlights.core.plugin.PluginWrapper;
import me.footlights.ui.web.ajax.AjaxServer;

import static me.footlights.core.Log.log;
import static me.footlights.ui.web.Constants.*;


public class WebUI extends me.footlights.core.UI
{
	public WebUI(Footlights footlights)
	{
		super("Web UI", footlights);

		this.port = WEB_PORT;

		log("Starting server() on port " + port +  "...");
		server = new MasterServer(port, footlights, new AjaxServer(footlights));
	}


	public void run()
	{
		new Thread(null, server, "Web Server").run();
	}


	@Override public void pluginLoaded(PluginWrapper plugin)
	{
//		ui.setStatus("Loaded plugin '" + plugin.getName() + "'.");
	}

	@Override public void pluginUnloading(PluginWrapper plugin)
	{
//		ui.setStatus("Unloading plugin '" + plugin.getName() + "'.");
	}


	/** The main web server */
	private MasterServer server;

	/** The TCP/IP port we're serving from */
	private final int port;
}

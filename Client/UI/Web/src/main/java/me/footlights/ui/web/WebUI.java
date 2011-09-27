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

import java.util.Map;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import me.footlights.core.Footlights;
import me.footlights.core.plugin.PluginWrapper;

import static me.footlights.ui.web.Constants.*;


public class WebUI extends me.footlights.core.UI
{
	public static WebUI init(Footlights footlights)
	{
		int port = WEB_PORT;
		log.info("Using TCP port " + port);

		Map<String, PluginWrapper> plugins = Maps.newLinkedHashMap();

		AjaxServer ajax = new AjaxServer(footlights);
		StaticContentServer staticContent = new StaticContentServer(plugins);
		MasterServer master = new MasterServer(port, footlights, ajax, staticContent);

		return new WebUI(footlights, master, plugins);
	}

	private WebUI(Footlights footlights, MasterServer server, Map<String, PluginWrapper> plugins)
	{
		super("Web UI", footlights);

		this.plugins = plugins;
		this.server = server;
	}


	public void run()
	{
		new Thread(null, server, "Web Server").run();
	}


	@Override public void pluginLoaded(PluginWrapper plugin)
	{
		plugins.put(plugin.getPluginName(), plugin);
//		ui.setStatus("Loaded plugin '" + plugin.getName() + "'.");
	}

	@Override public void pluginUnloading(PluginWrapper plugin)
	{
		plugins.remove(plugin.getPluginName());
//		ui.setStatus("Unloading plugin '" + plugin.getName() + "'.");
	}


	/** Log. */
	private static final Logger log = Logger.getLogger(WebUI.class.getName());

	/** The main web server */
	private MasterServer server;

	/** Currently-loaded plugins. */
	private final Map<String, PluginWrapper> plugins;
}

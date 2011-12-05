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
import me.footlights.core.Preconditions;
import me.footlights.core.apps.AppWrapper;

import static me.footlights.ui.web.Constants.*;


public class WebUI extends me.footlights.core.UI
{
	public static WebUI init(Footlights footlights)
	{
		Preconditions.notNull(footlights);

		int port = WEB_PORT;
		log.info("Using TCP port " + port);

		Map<String, AppWrapper> apps = Maps.newLinkedHashMap();

		AjaxServer ajax = new AjaxServer(footlights);
		StaticContentServer staticContent = new StaticContentServer(apps);
		MasterServer master = new MasterServer(port, footlights, ajax, staticContent);

		return new WebUI(footlights, master, apps);
	}

	private WebUI(Footlights footlights, MasterServer server, Map<String, AppWrapper> apps)
	{
		super("Web UI", footlights);

		this.apps = apps;
		this.server = server;
	}


	public void run()
	{
		new Thread(null, server, "Web Server").run();
	}


	@Override public void applicationLoaded(AppWrapper app) { apps.put(app.getName(), app); }
	@Override public void applicationUnloading(AppWrapper app) { apps.remove(app.getName()); }


	/** Log. */
	private static final Logger log = Logger.getLogger(WebUI.class.getName());

	/** The main web server */
	private MasterServer server;

	/** Currently-loaded applications. */
	private final Map<String, AppWrapper> apps;
}

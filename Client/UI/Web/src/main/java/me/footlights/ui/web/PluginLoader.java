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

import java.net.URI;

import me.footlights.core.Footlights;
import me.footlights.core.plugin.PluginWrapper;
import me.footlights.plugin.AjaxHandler;
import me.footlights.plugin.JavaScript;
import me.footlights.plugin.WebRequest;


public class PluginLoader implements AjaxHandler
{
	PluginLoader(Footlights footlights)
	{
		this.footlights = footlights;
	}

	@Override
	public JavaScript service(WebRequest request) throws Throwable
	{
		String name = request.path();

		PluginWrapper plugin = footlights.loadPlugin(name, new URI(request.path()));
		plugin.run(footlights);

		JavaScript response = new JavaScript();
		response.append("console.log('\"");
		response.append(plugin.getPluginName());
		response.append("\" loaded');");

		return response;
	}


	private final Footlights footlights;
}

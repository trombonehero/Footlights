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
import me.footlights.plugin.Context;
import me.footlights.plugin.JavaScript;
import me.footlights.plugin.WebRequest;


public class PluginLoader implements AjaxHandler
{
	PluginLoader(Footlights footlights, Context ajaxContext)
	{
		this.footlights = footlights;
		this.ajaxContext = ajaxContext;
	}

	@Override
	public JavaScript service(WebRequest request) throws Throwable
	{
		String name = request.path().substring(request.path().lastIndexOf('/') + 1);

		JavaScript response = new JavaScript();

		PluginWrapper plugin = footlights.loadPlugin(name, new URI(request.path()));
		response.append("rootContext.log('loaded plugin \\'");
		response.append(name.substring(name.lastIndexOf('.') + 1));
		response.append("\\'');");

		response.append("console.log('\"");
		response.append(JavaScript.sanitizeText(plugin.getPluginName()));
		response.append("\" loaded as \"");
		response.append(name);
		response.append(")');");

		ajaxContext.register(name, plugin.getWrappedPlugin().ajaxHandler());

		response.append("var sb = sandboxes.create('plugin/");
		response.append(plugin.getPluginName());
		response.append("', rootContext, rootContext.log, 0, 0, 200, 200);");

		response.append("sb.ajax('init');");

		return response;
	}


	private final Footlights footlights;
	private final Context ajaxContext;
}

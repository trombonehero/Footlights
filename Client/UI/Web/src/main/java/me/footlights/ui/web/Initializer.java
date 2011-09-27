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

import me.footlights.plugin.AjaxHandler;
import me.footlights.plugin.JavaScript;
import me.footlights.plugin.WebRequest;

import static me.footlights.plugin.JavaScript.ajax;


/** Initializes the UI after the static JavaScript has loaded. */
class Initializer implements AjaxHandler
{
	Initializer(AjaxServer ajaxServer) { this.ajaxServer = ajaxServer; }

	@Override public JavaScript service(WebRequest request)
	{
		ajaxServer.reset();

		JavaScript script = new JavaScript();

		script.append("var buttons = document.getElementById('buttons');");
		script.append("buttons.innerHTML='';");

		script.append(button("Good Plugin", ajax("load_plugin/" + GOOD_PLUGIN)));
		script.append(button("Wicked Plugin", ajax("load_plugin/" + WICKED_PLUGIN)));

		script.append(button("Reset", ajax("reset")));

		script.append("console.log('UI Initialized');");

		return script;
	}


	private static JavaScript button(String label, JavaScript onClick)
	{
		JavaScript script = new JavaScript();

		script.append("var button = document.createElement('button');");
		script.append("button.type = 'button';");

		script.append("button.appendChild(document.createTextNode('");
		script.append(label);
		script.append("'));");

		script.append("button.onclick = function() {");
		script.append(onClick);
		script.append("};");

		script.append("buttons.appendChild(button);");

		return script;
	}

	// Hardcode plugin paths for now, just to demonstrate that they work.
	private static final String CORE_PATH = System.getProperty("java.class.path").split(":")[0];
	private static final String PLUGIN_PATH = CORE_PATH.replaceFirst("Bootstrap/.*", "Plugins/");

	private static final String GOOD_PLUGIN =
		"file:" + PLUGIN_PATH
		+ "Good/target/classes!/me.footlights.demo.plugins.good.GoodPlugin";

	private static final String WICKED_PLUGIN =
		"jar:file:" + PLUGIN_PATH
		+ "Wicked/target/wicked-plugin-HEAD.jar!/me.footlights.demo.plugins.wicked.WickedPlugin";

	/** The Ajax server that needs to be reset. */
	private final AjaxServer ajaxServer;
}

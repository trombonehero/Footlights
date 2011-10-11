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

import _root_.me.footlights.core.Footlights;
import _root_.me.footlights.plugin.AjaxHandler;
import _root_.me.footlights.plugin.JavaScript;
import _root_.me.footlights.plugin.WebRequest;



/** The global context - code sent here has full DOM access. */
class GlobalContext(footlights:Footlights, server:AjaxServer)
	extends AjaxHandler
{
	override def service(request:WebRequest) = {
		request.path() match {
			case "init" => {
				server.reset()

				new JavaScript()
					.append("""
var buttons = document.getElementById('buttons');
buttons.innerHTML='';""")

					.append(button("Good Plugin", JavaScript.ajax("load_plugin/" + GOOD_PLUGIN)))
					.append(button("Wicked Plugin", JavaScript.ajax("load_plugin/" + WICKED_PLUGIN)))
					.append(button("Tic-Tac-Toe", JavaScript.ajax("load_plugin/" + TICTACTOE)))

					.append(button("Reset", JavaScript.ajax("reset")))

					.append(JavaScript.ajax("load_plugin/" + TICTACTOE))
					/*
					.append(ajax("load_plugin/" + GOOD_PLUGIN))
					.append(ajax("load_plugin/" + WICKED_PLUGIN))
					 */

					.append("console.log('UI Initialized');")
			}

			case "reset" => {
				while (footlights.plugins().size() > 0)
					footlights.unloadPlugin(
						footlights.plugins().iterator().next());

				server.reset();
				new JavaScript().append("window.location.reload()");
			}

			case LoadPlugin(path) => {
				val name = path.substring(path.lastIndexOf('/') + 1);
				val uri = new java.net.URI(request.shift().path())
				val plugin = footlights.loadPlugin(name, uri)

				server.register(name, plugin.getWrappedPlugin.ajaxHandler)

				new JavaScript()
					.append("""
rootContext.log('loaded plugin \'""" + name.substring(name.lastIndexOf('.') + 1) + """');

console.log('"""").appendText(plugin.getPluginName())
	.append("""" loaded as """").appendText(name).append(""""');

var sb = sandboxes.create('plugin/""")
	.appendText(plugin.getPluginName())
	.append("""', rootContext, rootContext.log, 0, 0, 200, 200);

sb.ajax('init');""")
			}
		}
	}

	private val LoadPlugin = """load_plugin/(.*)""".r

	private def button(label:String, onClick:JavaScript) = new JavaScript()
		.append("""
var button = document.createElement('button');
button.type = 'button';

button.appendChild(document.createTextNode('""").appendText(label).append("""'));
button.onclick = function() { """).append(onClick).append("""};

buttons.appendChild(button);""")



	// Hardcode plugin paths for now, just to demonstrate that they work.
	private val CORE_PATH = System.getProperty("java.class.path").split(":")(0)
	private val PLUGIN_PATH = "file:" + CORE_PATH.replaceFirst("Bootstrap/.*", "Plugins/")

	private val GOOD_PLUGIN = PLUGIN_PATH +
		"Good/target/classes!/me.footlights.demo.plugins.good.GoodPlugin"

	private val WICKED_PLUGIN = "jar:" + PLUGIN_PATH +
		"Wicked/target/wicked-plugin-HEAD.jar!/me.footlights.demo.plugins.wicked.WickedPlugin"

	private val TICTACTOE = PLUGIN_PATH +
		"TicTacToe/target/classes!/me.footlights.demo.tictactoe.TicTacToe"
}

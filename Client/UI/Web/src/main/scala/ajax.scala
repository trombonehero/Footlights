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

import me.footlights.api.WebRequest;
import me.footlights.api.ajax.AjaxHandler;
import me.footlights.api.ajax.AjaxResponse;
import me.footlights.api.ajax.JavaScript;

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import _root_.me.footlights.core.Footlights;
import _root_.me.footlights.api.WebRequest;
import _root_.me.footlights.api.ajax.{AjaxHandler,JavaScript}



/** Acts as an Ajax server for the JavaScript client */
class AjaxServer(footlights:Footlights) extends WebServer
{
	val appAjaxHandlers = new HashMap[String, AjaxHandler]
	val globalContext = new GlobalContext(footlights, this)

	override def name:String = "Ajax"

	override def handle(request:WebRequest) = {
		val resp =
			request.prefix() match {
				case "global" => globalContext.service(request.shift())
				case "app" => {
					val appRequest = request.shift()

					appAjaxHandlers.get(appRequest.prefix())
						.getOrElse(throw new IllegalArgumentException(
							"No such app '" + appRequest.prefix() + "'"))
						.service(appRequest.shift())
				}
				case _:String => new JavaScript()
			}

		Response.newBuilder()
			.setResponse(resp.mimeType, resp.data)
			.build()
	}


	def reset = appAjaxHandlers.clear();
	def register(name:String, appHandler:AjaxHandler)
	{
		if (appAjaxHandlers.contains(name))
			throw new RuntimeException(name + " already registered");

		appAjaxHandlers += name -> appHandler
	}
}



/** The global context - code sent here has full DOM access. */
class GlobalContext(footlights:Footlights, server:AjaxServer)
	extends AjaxHandler
{
	override def service(request:WebRequest) = {
		request.path() match {
			case "init" => {
				server.reset

				new JavaScript()
					.append("""
var buttons = context.root.getChild(function(node) { return node.class == 'buttons'; });
buttons.clear();""")

					.append(button("Good App", JavaScript.ajax("load_app/" + GOOD_APP)))
					.append(button("Wicked App", JavaScript.ajax("load_app/" + WICKED_APP)))
					.append(button("Tic-Tac-Toe", JavaScript.ajax("load_app/" + TICTACTOE)))

					.append(button("Reset", JavaScript.ajax("reset")))

					.append("context.log('UI Initialized');")
			}

			case "reset" => {
				while (footlights.runningApplications().size() > 0)
					footlights.unloadApplication(
						footlights.runningApplications().iterator().next());

				server.reset
				new JavaScript().append("context.globals['window'].location.reload()")
			}

			case LoadApplication(path) => {
				val name = path.substring(path.lastIndexOf('/') + 1);
				val uri = new java.net.URI(request.shift().path())
				val app = footlights.loadApplication(name, uri)

				server.register(name, app.getApp.ajaxHandler)

				new JavaScript()
					.append("""
context.log('loaded app \'""" + name.substring(name.lastIndexOf('.') + 1) + """\'');

var sb = context.globals['sandboxes'].create('app/""")
	.appendText(app.getName())
	.append("""', context.root, context.log, { x: 0, y: 0, width: 600, height: 400 });

sb.ajax('init');""")
			}

			case FillPlaceholder(name) => {
				me.footlights.api.ajax.JSON.newBuilder()
					.put("key", name)
					.put("value", "the user's name")
					.build()
			}
		}
	}

	private val FillPlaceholder = """fill_placeholder/(.*)""".r
	private val LoadApplication = """load_app/(.*)""".r

	private def button(label:String, onClick:JavaScript) = new JavaScript()
		.append("""
var button = buttons.appendElement('button');
button.appendText('""").appendText(label).append("""');

button.onclick = function() { """).append(onClick).append("""};
""")



	// Hardcode demo app paths for now, just to demonstrate that they work.
	private val CORE_PATH = System.getProperty("java.class.path").split(":")(0)
	private val APP_PATH = "file:" + CORE_PATH.replaceFirst("Bootstrap/.*", "Demos/")

	private val GOOD_APP = APP_PATH +
		"Basic/target/classes!/me.footlights.demos.good.GoodApp"

	private val WICKED_APP = "jar:" + APP_PATH +
		"Wicked/target/wicked-app-HEAD.jar!/me.footlights.demos.wicked.WickedApp"

	private val TICTACTOE = APP_PATH +
		"TicTacToe/target/classes!/me.footlights.demos.tictactoe.TicTacToe"
}

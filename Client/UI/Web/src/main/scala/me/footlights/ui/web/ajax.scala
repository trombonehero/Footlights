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
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import me.footlights.api.WebRequest
import me.footlights.api.ajax.{AjaxHandler, AjaxResponse, JavaScript}

import me.footlights.core.Footlights

package me.footlights.ui.web {

/** Acts as an Ajax server for the JavaScript client */
class AjaxServer(footlights:Footlights) extends WebServer
{
	val appAjaxHandlers = new HashMap[String, AjaxHandler]
	val globalContext = new GlobalContext(footlights, this)

	override def name:String = "Ajax"

	override def handle(req:WebRequest) = {
		// The request to be passed up to the next level of the stack.
		val request = req.shift

		Option(req.prefix match {
			case GlobalContext => globalContext service request
			case ApplicationContext =>
				appAjaxHandlers.get(request.prefix) map { _.service(request.shift) } getOrElse {
					throw new IllegalArgumentException("No such app: " + request.prefix)
				}
			case _:String => new JavaScript()
		}) map { response =>
			Response.newBuilder()
				.setResponse(response.mimeType, response.data)
				.build()
		} getOrElse { throw new IllegalArgumentException("Unable to service request: " + req) }
	}


	def reset = appAjaxHandlers.clear();
	def register(name:String, appHandler:AjaxHandler)
	{
		if (appAjaxHandlers.contains(name))
			throw new RuntimeException(name + " already registered");

		appAjaxHandlers += name -> appHandler
	}

	/** Send an asychronous event to the trusted part of the UI. */
	private[web] def fireEvent(response:JavaScript) = globalContext fireEvent response

	private val GlobalContext = "global"
	private val ApplicationContext = "app"
}



/** The global context - code sent here has full DOM access. */
class GlobalContext(footlights:Footlights, server:AjaxServer)
	extends AjaxHandler
{
	override def service(request:WebRequest) = {
		request.path() match {
			case Init =>
				server.reset

				val launcher = "context.globals['launcher']"

				new JavaScript()
					.append(createClickableText(launcher, "Reset", "reset"))
					.append(createClickableText(launcher, "Basic Demo", "load_app/" + GOOD_APP))
					.append(createClickableText(launcher, "Tic-Tac-Toe", "load_app/" + TICTACTOE))
					.append(createClickableText(launcher, "Wicked Demo", "load_app/" + WICKED_APP))
					.append(createClickableText(launcher, "File Uploader", "load_app/" + WICKED_APP))
					.append(setupAsyncChannel)

					.append("""
var contents =
	context.globals['sandboxes'].create(
		'content', context.globals['content'], context.log, { x: 0, y: 0 });
""")
					.append(listFiles)
					.append("context.log('UI initialized.');")

			case Reset =>
				while (footlights.runningApplications().size() > 0)
					footlights.unloadApplication(
						footlights.runningApplications().iterator().next());

				server.reset
				new JavaScript().append("context.globals['window'].location.reload()")

			case AsyncChannel =>
				asyncEvents.synchronized {
					while (asyncEvents.isEmpty) { asyncEvents.wait }
					new JavaScript()
						.append(setupAsyncChannel)
						.append(asyncEvents.dequeue)
				}

			case LoadApplication(path) =>
				val name = path.substring(path.lastIndexOf('/') + 1);
				val uri = new java.net.URI(request.shift().path())
				val app = footlights.loadApplication(name, uri)

				server.register(name, app.getApp.ajaxHandler)
				createUISandbox(app.getName)

			case FillPlaceholder(name) => {
				me.footlights.api.ajax.JSON.newBuilder()
					.put("key", name)
					.put("value", footlights.evaluate(name))
					.build
			}
		}
	}


	private def createClickableText(parent:String, name:String, ajax:String) = {
		new JavaScript()
			.append("""
var a = %s.appendElement('div').appendElement('a');
a.appendText('%s');
a.onclick = function onClickHandler() { context.ajax('%s'); };
""" format (parent, JavaScript.sanitizeText(name), ajax))
	}

	private def createUISandbox(name:String) =
		new JavaScript()
			.append("""
var sb = context.globals['sandboxes'].create(
	'app/%s', context.globals['content'], context.log, { x: 0, y: 0 });
sb.ajax('init');
""" format (JavaScript.sanitizeText(name), "100%"))


	private def listFiles = {
		val files = footlights.listFiles

		val js = new JavaScript()
		js.append("""
var dir = contents.root.appendElement('div');
dir.class = 'directory';
dir.style['font-family'] = 'monospace';
dir.style['white-space'] = 'pre-wrap';
dir.clear();
dir.appendElement('div').appendText('%d files in local cache:');""" format files.size)

		for (stat <- files take 5)
			js.append("dir.appendText('%10d B   %s\\n');"
					format (stat.length, JavaScript.sanitizeText(stat.name.encode)))

		js
	}

	private val asyncEvents = collection.mutable.Queue[JavaScript]()

	private[web] def fireEvent(event:JavaScript) = asyncEvents.synchronized {
		asyncEvents enqueue event
		asyncEvents notifyAll
	}

	private val Init            = "init"
	private val AsyncChannel    = "async_channel"
	private val Reset           = "reset"
	private val FillPlaceholder = """fill_placeholder/(.*)""".r
	private val LoadApplication = """load_app/(.*)""".r

	private val setupAsyncChannel =
		new JavaScript().append("context.globals['setupAsyncChannel']();")


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

}

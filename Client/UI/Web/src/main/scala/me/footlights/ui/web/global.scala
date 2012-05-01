/*
 * Copyright 2012 Jonathan Anderson
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
package me.footlights.ui.web

import java.net.URI
import java.util.logging

import me.footlights.api.WebRequest
import me.footlights.api.ajax._
import me.footlights.api.ajax.JSON._
import me.footlights.api.support.Either._
import me.footlights.api.support.Regex._

import me.footlights.core


/** The global context - code sent here has full DOM access. */
class GlobalContext(footlights:core.Footlights, reset:() => Unit,
		newContext:core.apps.AppWrapper => Unit)
		extends Context(classOf[GlobalContext]) {

	override def defaultResponse() = {
		val data = (this.getClass getResource "index.html").openStream
		Response.newBuilder setMimeType "text/html" setResponse data build
	}

	override val name = "Global context"
	override def openFile(name:String) = footlights open name map { case f:core.data.File => f }
	override def handleAjax(request:WebRequest):AjaxResponse = {
		request.path() match {
			case Init =>
				reset

				val launcher = "context.globals['launcher']"

				val js = new JavaScript()
					.append(createClickableText(launcher, "Reset", Reset))
					.append(createClickableText(launcher, "Load App", PromptApplication))
					.append(launcher + ".appendElement('hr');")

				footlights.applications map {
					case Left(ex) => JavaScript log ex.toString
					case Right((name, classpath)) =>
						createClickableText(launcher, name, LoadApplication substitute classpath)
				} foreach js.append

				js
					.append(setupAsyncChannel)
					.append("context.log('UI initialized.');")
					.append(JavaScript log "identities: " + footlights.identities)

			case Reset =>
				while (!footlights.runningApplications.isEmpty)
					footlights unloadApplication footlights.runningApplications.head

				reset()
				new JavaScript().append("context.globals['window'].location.reload()")

			case AsyncChannel =>
				asyncEvents.synchronized {
					while (asyncEvents.isEmpty) { asyncEvents.wait }
					asyncEvents.notify
					new JavaScript()
						.append(setupAsyncChannel)
						.append(asyncEvents.dequeue)
				}

			case PromptApplication =>
				footlights promptUser "Application class path" map {
					"load_app/%s".format(_) } map
					JavaScript.ajax fold(ex => JavaScript log ex.toString, js => js)

			case LoadApplication(path) =>
				val uri = new java.net.URI(request.shift().path())
				footlights.loadApplication(uri) match {
					case Right(wrapper) =>
						newContext(wrapper)
						createUISandbox(wrapper.name)

					case Left(error) =>
						log log (logging.Level.WARNING, "Error loading application", error)
						val stackTrace = new java.io.StringWriter
						error printStackTrace new java.io.PrintWriter(stackTrace)

						new JavaScript()
							.append("context.log('Error loading application:\\n%s');" format
									(JavaScript sanitizeText stackTrace.toString)
								)
				}

			case FillPlaceholder(name) => {
				JSON(
					"key" -> name,
					"value" -> ((footlights evaluate name getOrElse "(unknown)"):String)
				)
			}
		}
	}


	private val asyncEvents = collection.mutable.Queue[JavaScript]()
	private[web] def fireEvent(event:JavaScript) = asyncEvents.synchronized {
		asyncEvents enqueue event
		asyncEvents notify
	}



	private def createClickableText(parent:String, name:String, ajax:String) = {
		new JavaScript()
			.append("""
var a = %s.appendElement('div').appendElement('a');
a.appendText('%s');
a.onclick = function onClickHandler() { context.ajax('%s'); };
""" format (parent, JavaScript.sanitizeText(name), ajax))
	}

	private def createUISandbox(name:URI) =
		new JavaScript()
			.append("""
var sb = context.globals['sandboxes'].create(
	'%s', context.globals['content'], context.log, { x: 0, y: 0 });
sb.ajax('init');
""" format (JavaScript sanitizeText java.net.URLEncoder.encode(name.toString, "utf-8"), "100%"))



	private val Init            = "init"
	private val AsyncChannel    = "async_channel"
	private val Reset           = "reset"
	private val FillPlaceholder = """fill_placeholder/(\S+)""".r
	private val PromptApplication = "prompt_for_app"
	private val LoadApplication = """load_app/(\S+)""".r

	private val setupAsyncChannel =
		new JavaScript().append("context.globals['setupAsyncChannel']();")

	private val log = logging.Logger getLogger classOf[GlobalContext].getCanonicalName
}

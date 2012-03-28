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
import java.io.FileNotFoundException
import java.net.{InetSocketAddress,URI}
import java.nio.ByteBuffer
import java.nio.channels.{ServerSocketChannel,SocketChannel}
import java.util.logging.Level.{FINE,INFO,WARNING,SEVERE}
import java.util.logging.Logger

import scala.actors.Actor._
import scala.actors.Futures._

import me.footlights.api.WebRequest
import me.footlights.api.ajax.{AjaxResponse,JavaScript,JSON}
import me.footlights.api.ajax.JSON._
import me.footlights.core.Footlights
import me.footlights.core.apps.AppWrapper
import me.footlights.core.data


package me.footlights.ui.web {

/**
 * Server-side handle to a client sandbox's context.
 *
 * @param  base    the base app or Footlights class, relative to which we load e.g. resources
 */
abstract class Context(base:Class[_]) extends WebServer {
	/** Concrete subclasses must handle Ajax. */
	protected def handleAjax(req:WebRequest): AjaxResponse

	/** Open a {@link data.File}, perhaps using a app-specific keychain. */
	protected def openFile(name:String): Option[data.File]

	/** Subclasses <i>may</i> provide a default response for empty requests. */
	protected def defaultResponse() = Response error new FileNotFoundException

	override def handle(req:WebRequest) = {
		val remainder = req.shift

		req.prefix match {
			case Ajax =>
				val response = handleAjax(remainder)
				Response.newBuilder
					.setResponse(response.mimeType, response.data)
					.build

			case File =>
				val r = Response.newBuilder
				openFile(remainder.path) foreach { file => r setResponse file.getInputStream }
				r.build

			case StaticContent =>
				val mimeType = MimeType(remainder.path)
				Response.newBuilder
					.setMimeType(MimeType(remainder.path))
					.setResponse(getStaticContent(remainder))
					.build

			case _ => defaultResponse
		}
	}

	private def getStaticContent(request:WebRequest) = {
		val path = request.path
		if (path contains "..") throw new SecurityException("'..' present in " + request)

		val url = base getResource path
		if (url == null) throw new FileNotFoundException(path)

		url.openStream
	}

	private val Ajax = "ajax"
	private val File = "file"
	private val StaticContent = "static"

	private def log = Logger.getLogger(classOf[MasterServer].getCanonicalName)
}


/** The Ajax, etc. context for an unprivileged application. */
class AppContext(wrapper:AppWrapper) extends Context(wrapper.app.getClass) {
	override val name = "Application context: '%s'" format wrapper.name
	override def handleAjax(req:WebRequest) = wrapper.app.ajaxHandler service req
	override def openFile(name:String) = wrapper.kernel open name map { case f:data.File => f }
}


/** The global context - code sent here has full DOM access. */
class GlobalContext(footlights:Footlights, reset:() => Unit, newContext:AppWrapper => Unit)
		extends Context(classOf[GlobalContext]) {

	override def defaultResponse() = {
		val data = (this.getClass getResource "index.html").openStream
		Response.newBuilder setMimeType "text/html" setResponse data build
	}

	override val name = "Global context"
	override def openFile(name:String) = footlights open name map { case f:data.File => f }
	override def handleAjax(request:WebRequest):AjaxResponse = {
		request.path() match {
			case Init =>
				reset

				val launcher = "context.globals['launcher']"

				new JavaScript()
					.append(createClickableText(launcher, "Reset", "reset"))
					.append(createClickableText(launcher, "Basic Demo", "load_app/" + GOOD_APP))
					.append(createClickableText(launcher, "Tic-Tac-Toe", "load_app/" + TICTACTOE))
					.append(createClickableText(launcher, "Wicked Demo", "load_app/" + WICKED_APP))
					.append(createClickableText(launcher, "File Uploader", "load_app/" + UPLOADER))
					.append(createClickableText(launcher, "Photo Manager", "load_app/" + PHOTO_APP))
					.append(setupAsyncChannel)
					.append("context.log('UI initialized.');")

			case Reset =>
				while (footlights.runningApplications().size() > 0)
					footlights.unloadApplication(
						footlights.runningApplications().iterator().next());

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

			case LoadApplication(path) =>
				val uri = new java.net.URI(request.shift().path())
				footlights.loadApplication(uri) match {
					case Right(wrapper) =>
						newContext(wrapper)
						createUISandbox(wrapper.name)

					case Left(error) =>
						log log (WARNING, "Error loading application", error)
						val stackTrace = new java.io.StringWriter
						error printStackTrace new java.io.PrintWriter(stackTrace)

						new JavaScript()
							.append("context.log('Error loading application:\\n%s');" format
									(JavaScript sanitizeText stackTrace.toString)
								)
				}

			case FillPlaceholder(name) => {
				JSON("key" -> name, "value" -> footlights.evaluate(name))
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
	private val FillPlaceholder = """fill_placeholder/(.*)""".r
	private val LoadApplication = """load_app/(.*)""".r

	private val setupAsyncChannel =
		new JavaScript().append("context.globals['setupAsyncChannel']();")

	private val log = Logger getLogger classOf[GlobalContext].getCanonicalName

	// Hardcode demo app paths for now, just to demonstrate that they work.
	private val CORE_PATH = System.getProperty("java.class.path").split(":")(0)
	private val APP_PATH = "file:" + CORE_PATH.replaceFirst("Bootstrap/.*", "Demos/")

	private val GOOD_APP = APP_PATH + "Basic/target/classes"
	private val WICKED_APP = "jar:" + APP_PATH + "Wicked/target/wicked-app-HEAD.jar!/"
	private val PHOTO_APP = APP_PATH + "Photos/target/classes"
	private val TICTACTOE = APP_PATH + "TicTacToe/target/classes"
	private val UPLOADER = APP_PATH + "Uploader/target/classes"
}


object MimeType {
	def apply(path:String):String = {
		path match {
			case CSS(_)      => "text/css"
			case HTML(_)     => "text/html"
			case JS(_)       => "text/javascript"

			case GIF(_)      => "image/gif"
			case JPEG(_)     => "image/jpeg"
			case PNG(_)      => "image/png"

			case TrueType(_) => "font/ttf"

			case _           => "application/octet-stream"
		}
		
	}

	private val CSS      = """(.*)\.css""".r
	private val GIF      = """(.*)\.gif""".r
	private val HTML     = """(.*)\.html?""".r
	private val JPEG     = """(.*)\.jpe?g""".r
	private val JS       = """(.*)\.js""".r
	private val PNG      = """(.*)\.png""".r
	private val TrueType = """(.*)\.ttf""".r
}

}
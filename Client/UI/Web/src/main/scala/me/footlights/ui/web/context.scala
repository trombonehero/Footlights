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
import me.footlights.api.ajax.{AjaxResponse,JavaScript,JSON,URLEncoded}
import me.footlights.api.ajax.JSON._
import me.footlights.api.support.Either._
import me.footlights.api.support.Regex._
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

	/** Open a {@link data.File}, perhaps using an app-specific keychain. */
	protected def openFile(name:String): Either[Exception,data.File]

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
				openFile(URLEncoded(remainder.path).raw) foreach {
					file => r setResponse file.getInputStream
				}
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
	override def openFile(name:String) = wrapper.kernel open name map { case f:data.File => f }
	override def handleAjax(req:WebRequest) =
		wrapper.app.ajaxHandler map { _ service req } getOrElse {
			JavaScript log { "%s app has no Ajax handler" format wrapper.app.shortName }
		}
}

}
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

import java.io._
import java.net.{ServerSocket,Socket,SocketException}
import java.util.logging.Level.{FINE,WARNING,SEVERE}
import java.util.logging.Logger

import scala.actors.Actor._
import scala.actors.Futures._

import me.footlights.core.Footlights;

package me.footlights.ui.web {

/** Acts as a master server for Basic UI, JavaScript and Ajax */
class MasterServer(
		port:Int, footlights:Footlights, ajax:AjaxServer, staticServer:StaticContentServer)
	extends Runnable {

	val servers = Map(
			"" -> staticServer,
			"static" -> staticServer,
			"ajax" -> ajax
		)

	private var done = false

	override def run = {
		val serverSocket = new ServerSocket(port)

		do {
			log fine "Waiting for connection..."
			val socket = serverSocket.accept
			log fine "Accepted conncetion from " + socket

			val request = Option(socket.getInputStream) map { in =>
				new BufferedReader(new InputStreamReader(in))
			} flatMap { reader =>
				Option(reader.readLine)
			} map {
				Request parse
			}

			server ! (request,socket)
		} while (!done)
	}

	private val server = actor {
		loop {
			react {
				case (Some(request:Request), socket:Socket) => future {
					log fine "Request: " + request

					servers.get(request.prefix) map { server =>
						try { server handle request.shift }
						catch {
							case e:FileNotFoundException => Response error e
							case e:SecurityException =>
								log.log(WARNING, "Security error handling " + request, e)
								Response error e
	
							case t:Throwable =>
								log.log(SEVERE, "Unanticipated error handling " + request, t)
								Response error t
						}
					} orElse {
						Some(Response.error(new FileNotFoundException(request toString)))
					} foreach { response =>
						log fine "Response: " + response
						try { response write socket.getOutputStream }
						catch {
							case t:Throwable =>
								log.log(SEVERE,
									"Error responding to " + request + " with " + response, t)
						}
						socket.close
					}
				}

				case a:Any =>
					log info "Unknown event for 'server' actor: " + a
					Unit
			}
		}
	}


	private def log = Logger.getLogger(classOf[MasterServer].getCanonicalName)
}

}
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
import java.io.FileNotFoundException
import java.net.{InetSocketAddress,URLDecoder}
import java.nio.ByteBuffer
import java.nio.channels.{ServerSocketChannel,SocketChannel}
import java.util.logging.Level.{FINE,INFO,WARNING,SEVERE}
import java.util.logging.Logger

import scala.actors.Actor._
import scala.actors.Futures._

import me.footlights.core.Footlights


package me.footlights.ui.web {

class AbortedSessionException extends Exception("Current UI session aborted")


/** Acts as a master server for Basic UI, JavaScript and Ajax */
class MasterServer(port:Int, footlights:Footlights)
	extends Runnable {

	def register(context:(String, Context)) = contexts += context
	private var contexts:Map[String,Context] = Map()

	private var done = false

	override def run = {
		val serverSocket = ServerSocketChannel.open.socket
		serverSocket bind new InetSocketAddress(port)

		do {
			log fine "Waiting for connection..."
			val client = serverSocket.getChannel.accept
			log fine "Accepted conncetion from " + client

			val buffer = ByteBuffer allocate 1024
			val request = Option(client) map { _ read buffer } filter { _ > 0 } map {
				new String(buffer.array, 0, _)
			} map Request.parse

			server ! (request,client)
		} while (!done)
	}

	private val server = actor {
		loop {
			react {
				case (Some(request:Request), client:SocketChannel) => future {
					log fine "Request: " + request

					val response = {
						try {
							if (request.prefix.isEmpty) Option(contexts.head._2 handle request)
							else {
								contexts get request.prefix map { _ handle request.shift }
							} orElse Some {
								Response error new FileNotFoundException(
										"No such context '%s'" format request.prefix)
							}
						} catch {
							case e:AbortedSessionException => None
							case e:FileNotFoundException => Some(Response error e)
							case e:SecurityException =>
								log.log(WARNING, "Security error handling " + request, e)
								Some(Response error e)
	
							case t:Throwable =>
								log.log(SEVERE, "Unanticipated error handling " + request, t)
								Some(Response error t)
						}
					} getOrElse { Response error new FileNotFoundException(request toString) }

					log fine "Response: " + response
					try { response write client }
					catch {
						case t:Throwable =>
							log.log(WARNING,
								"Error responding to " + request + " with " + response, t)
					}

					client.close
				}

				case (None, client:SocketChannel) =>
					log info "Received 'None' request from client, closing channel"
					client.close

				case a:Any => log severe "Web UI server received unknown event: " + a
			}
		}
	}


	private def log = Logger.getLogger(classOf[MasterServer].getCanonicalName)
}

}
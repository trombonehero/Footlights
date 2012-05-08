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
import java.net.URI
import java.util.logging.Logger

import scala.actors.Futures.future
import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => MutableMap}

import me.footlights.core
import me.footlights.core.{Footlights,Preconditions,UI}
import me.footlights.core.UI._
import me.footlights.core.apps.AppWrapper

import me.footlights.api.ajax.JavaScript
import me.footlights.api.support.Either._


package me.footlights.ui.web {

class WebUI(
		footlights:Footlights, server:MasterServer, globalContext:GlobalContext,
		apps:MutableMap[URI,AppWrapper])
	extends UI("Web UI", footlights) {

	override def run = new Thread(null, server, "Web Server").run
	override def handleEvent(e:UI.Event) = e match {
		case e:AppLoadedEvent =>
			apps put(e.app.name, e.app)
			globalContext fireEvent {
				new JavaScript()
					.append("context.log('Loaded app: %s');" format e.app.name)
			}

		case e:AppUnloadingEvent =>
			apps remove e.app.name
			globalContext fireEvent {
				new JavaScript()
					.append("context.log('Unloaded app: %s');" format e.app.name)
			}

		case e:FileOpenedEvent =>
			globalContext fireEvent {
				new JavaScript()
					.append("context.log('opened: ")
					.appendText(e.file toString)
					.append("');")
			}

		case e:FileSavedEvent =>
			globalContext fireEvent {
				new JavaScript()
					.append("context.log('saved: ").appendText(e.file toString).append("');")
			}

		case e:UI.Event =>
			globalContext fireEvent {
				new JavaScript()
					.append("context.log('Unknown event: %s');" format e)
			}
	}

	override def promptUser(title:String, prompt:String, default:Option[String])
			(callback: Either[core.UIException,String] => Any) = {

		future { callback { globalContext.promptUser(title, prompt, callback, default) } }
		true
	}

	override def choose[A](title:String, prompt:String, options:Map[String,A],
			default:Option[(String,A)] = None)(callback: Either[core.UIException,A] => Any) = {

		future {
			callback {
				globalContext.choose(title, prompt, options.keys, default map { _._1 }) flatMap {
					options get _ toRight(new core.CanceledException)
				}
			}
		}
		true
	}
}

object WebUI {
	def init(footlights:Footlights) = {
		Preconditions.notNull(footlights)
		val port = WebPort
		log info { "Using TCP port " + port }

		val apps = MutableMap[URI,AppWrapper]()
		val master = new MasterServer(port, footlights)

		def reset(): Unit = master.synchronized {
			// TODO: break circular dependency, clear and re-register global context in master
		}

		def createAppContext(app:AppWrapper) =
			master register (app.name.toString -> new AppContext(app))

		val globalContext = new GlobalContext(footlights, reset, createAppContext)
		master register ("footlights" -> globalContext)

		new WebUI(footlights, master, globalContext, apps)
	}

	/** Log. */
	private val log = Logger.getLogger(classOf[WebUI].getCanonicalName())

	/** TODO: do something more clever than a hard-coded constant */
	private val WebPort = 4567;
}

}

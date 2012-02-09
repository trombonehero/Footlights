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
import java.util.logging.Logger

import scala.collection.JavaConversions._
import scala.collection.mutable.Map

import me.footlights.core.{Footlights,Preconditions,UI}
import me.footlights.core.UI._
import me.footlights.core.apps.AppWrapper

import me.footlights.api.ajax.JavaScript

import me.footlights.ui.web.Constants.WEB_PORT

package me.footlights.ui.web {

class WebUI(
		footlights:Footlights, server:MasterServer, ajax:AjaxServer, apps:Map[String,AppWrapper])
	extends UI("Web UI", footlights) {

	override def run = new Thread(null, server, "Web Server").run
	override def handleEvent(e:UI.Event) = e match {
		case e:AppLoadedEvent =>
			apps put(e.app.getName, e.app)
			ajax fireEvent {
				new JavaScript()
					.append("context.log('Loaded app: %s');" format e.app.getName)
			}

		case e:AppUnloadingEvent =>
			apps remove e.app.getName
			ajax fireEvent {
				new JavaScript()
					.append("context.log('Unloaded app: %s');" format e.app.getName)
			}

		case e:FileOpenedEvent =>
			ajax fireEvent {
				new JavaScript()
					.append("context.log('opened: ")
					.appendText(e.file toString)
					.append("');")
			}

		case e:FileSavedEvent =>
			ajax fireEvent {
				new JavaScript()
					.append("context.log('saved: ")
					.appendText(e.file toString)
					.append("');")
					.append("function isSandbox(node) { return (node.class == \"sandbox\"); };")
					.append("var contents = context.root.getChild(isSandbox);")
					.append("contents.appendElement('div').appendText('")
					.appendText(footlights.listFiles.size.toString)
					.appendText(" files in local cache")
					.append("');")
			}

		case e:UI.Event =>
			ajax fireEvent {
				new JavaScript()
					.append("context.log('Unknown event: %s');" format e)
			}
	}
}

object WebUI {
	def init(footlights:Footlights) = {
		Preconditions.notNull(footlights)
		val port = WEB_PORT
		log info { "Using TCP port " + port }

		val apps:Map[String,AppWrapper] = Map()
		val ajax = new AjaxServer(footlights)
		val staticContent = new StaticContentServer(apps)
		val master = new MasterServer(port, footlights, ajax, staticContent)

		new WebUI(footlights, master, ajax, apps)
	}

	/** Log. */
	private val log = Logger.getLogger(classOf[WebUI].getCanonicalName())
}

}

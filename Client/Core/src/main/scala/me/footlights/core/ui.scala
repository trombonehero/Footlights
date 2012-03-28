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
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.logging.Logger

import javax.swing.JFileChooser

import scala.collection.mutable.Set

import me.footlights.api.{File,KernelInterface}
import me.footlights.api.support.Tee._


package me.footlights.core {

import apps.AppWrapper


/** A User Interface */
abstract class UI(val name:String, footlights:Footlights)
		extends Thread("Footlights UI: '" + name + "'") {

	def handleEvent(event:UI.Event): Unit

	footlights registerUI this
}

/** Companion object for {@link UI} (contains {@link UI.Event}, etc.). */
object UI {
	abstract class Event { def message: String }

	class AppLoadedEvent(val app:AppWrapper) extends Event {
		override val message = "Loaded app " + app
	}

	class AppUnloadingEvent(val app:AppWrapper) extends Event {
		override val message = "Unloading app " + app
	}

	class FileOpenedEvent(val file:File) extends Event {
		override val message = "Opened " + file
	}

	class FileSavedEvent(val file:File) extends Event {
		override val message = "Saved " + file
	}
}



/** Manages interaction with UIs (e.g. the Swing UI, the Web UI, ...). */
trait UIManager extends Footlights {
	protected def uis:Set[UI]

	override def registerUI(ui:UI):Unit = uis.add(ui)
	override def deregisterUI(ui:UI):Unit = uis.remove(ui)

	import UI._

	abstract override def open(filename:String) = {
		val f = super.open(filename)
		f map { new UI.FileOpenedEvent(_) } foreach fire
		f
	}

	abstract override def save(data:ByteBuffer) = {
		val f = super.save(data)
		f map { new UI.FileSavedEvent(_) } foreach fire
		f
	}

	abstract override def loadApplication(uri:URI) = {
		val wrapper = super.loadApplication(uri)
//		wrapper.left foreach { fire()}
		wrapper.right foreach { wrapper => fire(new AppLoadedEvent(wrapper)) }
		wrapper
	}

	abstract override def unloadApplication(app:AppWrapper) = {
		fire(new AppUnloadingEvent(app))
		super.unloadApplication(app)
	}

	private def fire(event: UI.Event) = uis foreach { _ handleEvent event }
}

/** Provides Swing-based powerboxes for prompting users (e.g. "which file?", "which friend?"). */
trait SwingPowerboxes extends Footlights {
	protected def io:IO

	override def openLocalFile():Option[_root_.me.footlights.api.File] = {
		val d = new JFileChooser
		val filename = d.showOpenDialog(null) match {
			case JFileChooser.APPROVE_OPTION => Option(d.getSelectedFile())
			case _ => { log.fine("User cancelled file open dialog"); None }
		}

		io read filename flatMap save tee { f => log fine ("Opened local file: %s" format f) }
	}

	override def saveLocalFile(file:me.footlights.api.File) = {
		val f = file match { case f:me.footlights.core.data.File => f }

		val d = new JFileChooser
		val filename = d.showSaveDialog(null) match {
			case JFileChooser.APPROVE_OPTION => Option(d.getSelectedFile)
			case _ => { log.fine("User cancelled save file dialog"); None }
		}

		filename map { localFileName => saveLocal(f, localFileName) }
	}

	private val log = Logger getLogger { classOf[SwingPowerboxes] getCanonicalName }
}

}

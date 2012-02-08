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
import java.util.logging.Logger

import javax.swing.JFileChooser

import scala.collection.mutable.Set

import me.footlights.api.{File,KernelInterface}


package me.footlights.core {

import apps.AppWrapper


/** Manages interaction with UIs (e.g. the Swing UI, the Web UI, ...). */
trait UIManager extends Footlights {
	protected def uis:Set[UI]

	override def registerUI(ui:UI):Unit = uis.add(ui)
	override def deregisterUI(ui:UI):Unit = uis.remove(ui)

	abstract override def open(filename:String):me.footlights.api.File = {
		val f = super.open(filename)
		fire(new FileOpenedEvent(f))
		f
	}

	abstract override def save(data:ByteBuffer):me.footlights.api.File = {
		val f = super.save(data)
		fire(new FileSavedEvent(f))
		f
	}

	abstract override def loadApplication(name:String, uri:URI) = {
		val wrapper = super.loadApplication(name, uri)
		fire(new AppLoadedEvent(wrapper))
		wrapper
	}

	abstract override def unloadApplication(app:AppWrapper) = {
		fire(new AppUnloadingEvent(app))
		super.unloadApplication(app)
	}

	private def fire(event: UI.Event) = uis foreach { _ handleEvent event }
}

/** Provides Swing-based powerboxes for prompting users (e.g. "which file?", "which friend?"). */
trait SwingPowerboxes extends Kernel {
	protected def io:IO
	override def openLocalFile():_root_.me.footlights.api.File = open getOrElse { null }


	private def open() = {
		val d = new JFileChooser
		val filename = d.showOpenDialog(null) match {
			case JFileChooser.APPROVE_OPTION => Option(d.getSelectedFile())
			case _ => { log.fine("User cancelled file open dialog"); None }
		}

		io.read(filename) map { data.File.newBuilder().setContent(_).freeze() }
	}

	private val log = Logger getLogger { classOf[SwingPowerboxes] getCanonicalName }
}

class AppLoadedEvent(a:AppWrapper) extends UI.Event {
	val app = a
	override val messageFOO = "Loaded app " + a
}

class AppUnloadingEvent(a:AppWrapper) extends UI.Event {
	val app = a
	override val messageFOO = "Unloading app " + a
}

class FileOpenedEvent(val file:File) extends UI.Event {
	override val messageFOO = "Opened " + file
}

class FileSavedEvent(val file:File) extends UI.Event {
	override val messageFOO = "Saved " + file
}

}

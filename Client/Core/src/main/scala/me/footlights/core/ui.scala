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

import javax.swing
import javax.swing.JFileChooser

import scala.actors.Futures.future
import scala.collection.mutable.Set

import me.footlights.api
import me.footlights.api.KernelInterface
import me.footlights.api.support.Either._
import me.footlights.api.support.Tee._


package me.footlights.core {

import apps.AppWrapper

class UIException(message:String) extends Exception(message)
class CanceledException(message:String = "User canceled operation") extends UIException(message)

/** A User Interface */
abstract class UI(val name:String, footlights:Footlights)
		extends Thread("Footlights UI: '" + name + "'") {

	def handleEvent(event:UI.Event): Unit

	/**
	 * Asks the UI to prompt the user for information.
	 *
	 * @return   whether or not the UI will commit to asking the user (if not, we will
	 *           carry on asking other UIs)
	 */
	def promptUser(title:String, prompt:String, default:Option[String] = None)
			(callback: Either[UIException,String] => Any): Boolean =
		false

	def choose[A](title:String, prompt:String,
			options:Map[String,A], default:Option[(String,A)] = None)
			(callback: Either[UIException,A] => Any) =
		false

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

	class FileOpenedEvent(val file:api.File) extends Event {
		override val message = "Opened " + file
	}

	class FileSavedEvent(val file:api.File) extends Event {
		override val message = "Saved " + file
	}
}



/** Manages interaction with UIs (e.g. the Swing UI, the Web UI, ...). */
trait UIManager extends Footlights {
	protected def uis:Set[UI]

	override def registerUI(ui:UI):Unit = uis.add(ui)
	override def deregisterUI(ui:UI):Unit = uis.remove(ui)

	import UI._

	abstract override def open(filename:URI) = {
		val f = super.open(filename)
		f map { new UI.FileOpenedEvent(_) } map fire
		f
	}

	abstract override def save(data:ByteBuffer) = {
		val f = super.save(data)
		f map { new UI.FileSavedEvent(_) } map fire
		f
	}

	abstract override def loadApplication(uri:URI) = {
		val wrapper = super.loadApplication(uri)
//		wrapper.left foreach { fire()}
		wrapper map { new AppLoadedEvent(_) } foreach fire
		wrapper
	}

	abstract override def unloadApplication(app:AppWrapper) = {
		fire(new AppUnloadingEvent(app))
		super.unloadApplication(app)
	}

	override def promptUser(prompt:String) = promptUser(prompt, None)
	override def promptUser(prompt:String, default:Option[String]) =
		promptUser(prompt, "Footlights", default)

	override def promptUser[A](promptText:String, title:String, options:Map[String,A],
			default:Option[(String,A)]) =
		prompt { uis.view map { ui => ui.choose(title, promptText, options, default) _} }

	private[core] override def promptUser(promptText:String, title:String, default:Option[String]) =
		prompt { uis.view map { ui => ui.promptUser(title, promptText, default) _ } }

	private def prompt[A](f:Iterable[(Either[UIException,A] => Any) => Boolean]) = {
		val done = new Object()
		var result:Either[UIException,A] =
			Left(new UIException("No UI capable of prompting user"))

		future {
			val callback = (response:Either[UIException,A]) => {
				result = response
				done.synchronized { done.notify }
			}

			val mission_accepted = f map { _(callback) } reduce { _ || _ }
			if (!mission_accepted) done.synchronized { done.notify }
		}

		done.synchronized { done.wait }
		result
	}

	private def fire(event: UI.Event) = uis foreach { _ handleEvent event }
}

/** Provides Swing-based powerboxes for prompting users (e.g. "which file?", "which friend?"). */
trait SwingPowerboxes extends Footlights {
	protected def io:IO

	override def openLocalFile():Either[Exception,api.File] = {
		val d = new JFileChooser
		val filename = d.showOpenDialog(null) match {
			case JFileChooser.APPROVE_OPTION => Right(d.getSelectedFile())
			case _ => Left[Exception,java.io.File](new CanceledException)
		}

		filename flatMap io.read flatMap save
	}

	override def saveLocalFile(file:me.footlights.api.File):Either[Exception,api.File] = {
		val f = file match { case f:me.footlights.core.data.File => f }

		val d = new JFileChooser
		val filename = d.showSaveDialog(null) match {
			case JFileChooser.APPROVE_OPTION => Right(d.getSelectedFile)
			case _ => Left[Exception,java.io.File](new CanceledException)
		}

		filename flatMap { localFileName => saveLocal(f, localFileName) }
	}

	private val log = Logger getLogger { classOf[SwingPowerboxes] getCanonicalName }
}

}

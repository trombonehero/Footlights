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

import java.io.{ByteArrayOutputStream, IOException, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer
import java.net.URI
import java.util.logging.{Level, Logger}

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import me.footlights.plugin.File
import me.footlights.plugin.KernelInterface
import me.footlights.plugin.ModifiablePreferences
import me.footlights.plugin.Plugin


package me.footlights.core {

import crypto.Keychain
import plugin.PluginLoadException
import plugin.PluginWrapper


/** Provides plugin [un]loading. */
trait Plugins extends Footlights {
	def keychain:Keychain
	def loadedPlugins:HashMap[URI,PluginWrapper]
	def pluginLoader:ClassLoader
	def prefs:FileBackedPreferences

	def open(name:String):File
	def save(bytes:ByteBuffer):File

	def plugins():_root_.java.util.Collection[PluginWrapper] = loadedPlugins.values

	def loadPlugin(name:String, uri:URI) = {
		loadedPlugins get(uri) getOrElse {
			val prefs = pluginPreferences(name)
			val c = pluginLoader.loadClass(uri.toString)
			val init = c.getMethod("init",
					classOf[KernelInterface], classOf[ModifiablePreferences], classOf[Logger])

			val plugin = try {
				init.invoke(null, this, prefs, Logger.getLogger(uri.toString())) match {
					case p:Plugin => p
					case o:Object => throw new ClassCastException(
							name + ".init() returned non-plugin '" + o + "'")
				}
			} catch {
				case e:Throwable => throw new PluginLoadException(uri, e)
			}

			val wrapper = new PluginWrapper(name, uri, plugin)
			loadedPlugins.put(uri, wrapper)

			wrapper
		}
	}

	def unloadPlugin(plugin:PluginWrapper) =
		loadedPlugins find { kv => kv._2 == plugin } foreach { kv => loadedPlugins remove kv._1 }


	/**
	 * Create a {@link ModifiablePreferences} for a plugin.
	 *
	 * This {@link ModifiablePreferences} object will start populated with saved preferences,
	 * if any exist, but it will also have the ability to save new preferences to a {@link File}.
	 */
	private def pluginPreferences(pluginName:String) = {
		val pluginKey = "plugin.prefs." + pluginName
		val filename =
			try Some(prefs.getString(pluginKey))
			catch { case e:NoSuchElementException => None }

		// Deserialize HashMap and explicitly convert its contents to strings.
		val loaded:Option[_root_.java.util.HashMap[_,_]] =
			filename map { f => new ObjectInputStream(open(f) getInputStream) readObject } flatMap {
				case i:_root_.java.util.HashMap[_,_] => Some(i)
				case o:Object => { log.severe("Stored prefs not a HashMap: " + o); None }
			}

		val toSave = new HashMap[String,String]
		loaded foreach { _ foreach { kv => toSave += ((kv._1 toString, kv._2 toString)) } }

		// Create an anonymous mutable version which can save itself
		val reader = PreferenceStorageEngine.wrap(toSave)

		new ModifiableStorageEngine {
			override def getAll = reader.getAll
			override def getRaw(name:String) = reader.getRaw(name)

			override def set(key:String, value:String) = synchronized
			{
				// Update the preferences.
				toSave.put(key, value)

				// Save updated preferences to the Store.
				val out = new ByteArrayOutputStream
				new ObjectOutputStream(out) writeObject toSave

				val saved = try Some {
					save(ByteBuffer.wrap(out.toByteArray())) match {
						case f:data.File => f
					}
				}
				catch { case e:IOException =>
					log.log(Level.WARNING, "Error saving preferences for '" + pluginName + "'", e)
					None
				}

				// Save the link to the updated preferences.
				saved map { _.link() } foreach { l => {
						l.saveTo(keychain)
						prefs.set(pluginKey, l.fingerprint.encode())
					}
				}

				this.notifyAll()
				this
			}
		}
	}

	private val log = Logger getLogger { classOf[Plugins] getCanonicalName }
}

}
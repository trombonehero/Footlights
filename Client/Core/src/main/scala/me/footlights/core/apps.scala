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
import java.io.{ByteArrayOutputStream, IOException}
import java.nio.ByteBuffer
import java.net.URI
import java.util.logging.{Level, Logger}

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

import me.footlights.api.Application
import me.footlights.api.File
import me.footlights.api.KernelInterface
import me.footlights.api.ModifiablePreferences

package me.footlights.core {

import apps.AppStartupException
import apps.AppWrapper
import crypto.Keychain


/** Provides plugin [un]loading. */
trait Applications extends Footlights {
	def keychain:Keychain
	def loadedApps:HashMap[URI,AppWrapper]
	def appLoader:ClassLoader
	def prefs:ModifiablePreferences

	def open(name:String):File
	def save(bytes:ByteBuffer):File

	def runningApplications():_root_.java.util.Collection[AppWrapper] = loadedApps.values

	def loadApplication(name:String, uri:URI) = {
		loadedApps get(uri) getOrElse {
			val prefs = appPreferences(name)
			val c = appLoader.loadClass(uri.toString)
			val init = c.getMethod("init",
					classOf[KernelInterface], classOf[ModifiablePreferences], classOf[Logger])

			val app = try {
				init.invoke(null, this, prefs, Logger.getLogger(uri.toString())) match {
					case a:Application => a
					case o:Object => throw new ClassCastException(
							name + ".init() returned non-application '" + o + "'")
				}
			} catch {
				case e:Throwable => throw new AppStartupException(uri, e)
			}

			val wrapper = new AppWrapper(name, uri, app)
			loadedApps.put(uri, wrapper)

			wrapper
		}
	}

	def unloadApplication(app:AppWrapper) =
		loadedApps find { kv => kv._2 == app } foreach { kv => loadedApps remove kv._1 }


	/**
	 * Create a {@link ModifiablePreferences} for an application.
	 *
	 * This {@link ModifiablePreferences} object will start populated with saved preferences,
	 * if any exist, but it will also have the ability to save new preferences to a {@link File}.
	 */
	private def appPreferences(appName:String) = {
		val appKey = "app.prefs." + appName
		val map = new HashMap[String,String]

		prefs getString(appKey) flatMap { filename =>
			try { Option(open(filename)) } catch { case e:data.NoSuchBlockException => None }
		} map {
			_ match { case file:data.File => map ++= Preferences.parse(file.getContents()) }
		}

		// Create an anonymous mutable version which can save itself
		val reader = PreferenceStorageEngine.wrap(map)

		new ModifiableStorageEngine {
			override def getAll = reader.getAll
			override def getRaw(name:String) = reader.getRaw(name)

			override def set(key:String, value:String) = synchronized
			{
				// Update the preferences.
				map.put(key, value)

				// Save updated preferences to the Store.
				val saved = try save(Preferences.encode(map)) match {
					case f:me.footlights.core.data.File => Some(f)
				} catch {
					case e:IOException =>
						log.log(Level.WARNING, "Error saving preferences for '" + appName + "'", e)
						None
				}

				// Save the link to the updated preferences.
				saved map { _.link() } foreach { l => {
						l.saveTo(keychain)
						prefs.set(appKey, l.fingerprint.encode())
					}
				}

				this
			}
		}
	}

	private val log = Logger getLogger { classOf[Applications] getCanonicalName }
}

}
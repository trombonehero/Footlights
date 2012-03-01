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
import java.net.{URI,URL}
import java.util.logging.{Level, Logger}

import scala.collection.JavaConversions._
import scala.collection.mutable

import me.footlights.api.Application
import me.footlights.api.File
import me.footlights.api.KernelInterface
import me.footlights.api.ModifiablePreferences

import me.footlights.api.support.Tee._


package me.footlights.core {

import apps.AppStartupException
import apps.AppWrapper
import crypto.Keychain


/** Provides plugin [un]loading. */
trait Applications extends Footlights {
	protected def keychain:Keychain
	protected def loadedApps:mutable.Map[URI,AppWrapper]
	protected def appLoader:ClassLoader
	protected def prefs:ModifiablePreferences

	protected def readPrefs(filename:String):Option[Map[String,String]]

	def runningApplications():_root_.java.util.Collection[AppWrapper] = loadedApps.values

	override def loadApplication(uri:URI) =
		loadedApps get(uri) getOrElse {
			val prefs = appPreferences(uri)

			loadAppClass(uri.toURL) flatMap findInit map { init =>
				try { init.invoke(null, this, prefs, Logger.getLogger(uri.toString)) match {
						case a:Application => a
						case o:Object => throw new ClassCastException(
								"init() returned non-application '" + o + "'")
					}
				} catch {
					case e:Throwable => throw new AppStartupException(uri, e)
				}
			} map {
				new AppWrapper(uri.toString, uri, _)
			} tee {
				loadedApps put (uri, _)
			} get
		}

	override def unloadApplication(app:AppWrapper) =
		loadedApps find { kv => kv._2 == app } foreach { kv => loadedApps remove kv._1 }


	/**
	 * Create a {@link ModifiablePreferences} for an application.
	 *
	 * This {@link ModifiablePreferences} object will start populated with saved preferences,
	 * if any exist, but it will also have the ability to save new preferences to a {@link File}.
	 */
	private def appPreferences(appName:URI) = {
		val appKey = "app.prefs." + appName
		val map = mutable.Map() ++ (prefs getString appKey flatMap readPrefs getOrElse Map())

		val reader = PreferenceStorageEngine wrap map

		// Create an anonymous mutable version which can save itself
		new ModifiableStorageEngine {
			override def getAll = reader.getAll
			override def getRaw(name:String) = reader.getRaw(name)

			override def set(key:String, value:String): ModifiableStorageEngine = synchronized
			{
				// Update the preferences.
				map.put(key, value)

				// Save updated preferences to the Store.
				val saved = save(Preferences.encode(map)) map { case f:data.File => f }
				if (saved.isEmpty) log warning "Failed to save preferences for '" + appName + "'"

				// Save the link to the updated preferences.
				saved map { _.link.fingerprint.encode } foreach { prefs.set(appKey, _) }

				this
			}
		}
	}


	/** Load an application's main class from a given classpath. */
	private def loadAppClass(classpath:URL):Option[Class[_]] =
		(try { loadApplicationMethod.invoke(appLoader, classpath) }
		catch {
			case t:Throwable => throw new AppStartupException(classpath.toURI, t)
		}) match {
			case c:Some[Class[_]] => c
			case a:Any =>
				throw new AppStartupException(classpath.toURI,
						new IllegalArgumentException("%s returned '%s', not a Class" format (
								loadApplicationMethod.getName, a))
					)
		}

	/** Find the "init" method for an application's main class. */
	private def findInit(c:Class[_]) =
		try {
			Some(c.getMethod("init",
				classOf[KernelInterface], classOf[ModifiablePreferences], classOf[Logger])
			)
		} catch {
			case t:Throwable =>
				throw new AppStartupException(new URI("class:" + c.getCanonicalName), t)
		}

	/**
	 * The method provided by the lower-level {@link ClassLoader} which actually loads applications.
	 *
	 * This will only succeed if we are running with appropriate JVM privileges (which we should,
	 * since this is part of {@link Kernel} initialization).
	 */
	private val loadApplicationMethod = appLoader.getClass.getDeclaredMethod(
			"loadApplication", classOf[URL])

	loadApplicationMethod.setAccessible(true)

	private val log = Logger getLogger { classOf[Applications] getCanonicalName }
}

}
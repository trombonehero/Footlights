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

import me.footlights.core.{Footlights,ModifiableStorageEngine}
import me.footlights.core.crypto.Keychain
import me.footlights.core.data


package me.footlights.core.apps {


/** Wrapper for applications; ensures consistent exception handling */
case class AppWrapper(
		mainClass:Class[_], val name:URI,
		val kernel:KernelInterface, val prefs:ModifiablePreferences, val log:Logger) {
	override lazy val toString = "Application { '" + name + "' }"

	/** The application itself (lazily initialized). */
	lazy val app:Application = {
		try {
			init.invoke(null, kernWrapper, prefs, log) match {
				case app:Application => app
				case a:Any => throw new ClassCastException(
						"init() returned non-application '%s'" format a)
			}
		} catch {
			case t:Throwable => throw new AppStartupException(name, t)
		}
	}

	private val kernWrapper:KernelInterface = new KernelInterface() {
		def save(bytes:ByteBuffer) =
			kernel save bytes tee { case f:data.File => appKeychain store f.link }

		def open(name:String) = try {
			appKeychain getLink { Fingerprint decode name } map kernel.open get
		}

		def openLocalFile = kernel.openLocalFile tee {
			case f:data.File => appKeychain store f.link
			println("Opened local:")
			println("name: " + f.name)
			println("key:  " + f.key)
		}
		def saveLocalFile(file:api.File) = kernel saveLocalFile file
	}

	/** The application's {@link Application#init} method. */
	private lazy val init = mainClass.getMethod("init",
			classOf[KernelInterface], classOf[ModifiablePreferences], classOf[Logger])
}


/** Provides plugin [un]loading. */
trait ApplicationManagement extends Footlights {
	protected def keychain:Keychain
	protected def loadedApps:mutable.Map[URI,AppWrapper]
	protected def appLoader:ClassLoader
	protected def prefs:ModifiablePreferences

	protected def readPrefs(filename:String):Option[Map[String,String]]

	def runningApplications():_root_.java.util.Collection[AppWrapper] = loadedApps.values

	override def loadApplication(uri:URI) =
		loadedApps get(uri) getOrElse {
			val prefs = appPreferences(uri)
			val appLog = Logger getLogger uri.toString

			loadAppClass(uri.toURL) map {
				AppWrapper(_, uri, this, prefs, appLog)
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

		ModifiableStorageEngine(map, Some(remember(appKey)))
	}

	/** Create a {@link Keychain} for an application which can save itself. */
	private def appKeychain(appName:URI) = {
		val appKey = "app.keychain." + appName
		val appKeychain = prefs getString appKey flatMap readKeychain getOrElse Keychain.create

		Flusher(appKeychain, save = remember(appKey)).start
		appKeychain
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


	/** Save some data, its {@link Link} (including symmetric key) and name. */
	private def remember(prefKey:String)(bytes:ByteBuffer) =
		save(bytes) map { case f:data.File => f } map { _.link } tee {
			_ saveTo keychain
		} map {
			_.fingerprint.encode
		} tee {
			prefs.set(prefKey, _)
		} orElse {
			log warning "Failed to save '" + prefKey + "'"
			None
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

	private val log = Logger getLogger { classOf[ApplicationManagement] getCanonicalName }
}

}
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
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.net.{URI,URL}
import java.security.AccessControlException
import java.util.logging.{Level, Logger}

import scala.collection.JavaConversions._
import scala.collection.mutable

import me.footlights.api
import me.footlights.api.{Application, KernelInterface, ModifiablePreferences}
import me.footlights.api.support.Pipeline._
import me.footlights.api.support.Tee._

import me.footlights.core
import me.footlights.core.{Flusher, Footlights, ModifiableStorageEngine}
import me.footlights.core.crypto.{Fingerprint, Keychain, MutableKeychain}
import me.footlights.core.data
import me.footlights.core.ProgrammerError


package me.footlights.core.apps {


/** Wrapper for applications; ensures consistent exception handling */
class AppWrapper private(init:Method, val name:URI, val kernel:KernelInterface,
		prefs:ModifiablePreferences, log:Logger) {
	override lazy val toString = "Application { '" + name + "' }"

	/** The application itself (lazily initialized). */
	lazy val app:Application = {
		try {
			init.invoke(null, kernel, prefs, log) match {
				case app:Application => app
				case a:Any => throw new ClassCastException(
						"init() returned non-application '%s'" format a)
			}
		} catch {
			case t:Throwable => throw new AppStartupException(name, t)
		}
	}
}

object AppWrapper {
	def apply(
			mainClass:Class[_], name:URI, footlights:Footlights, root:data.MutableDirectory,
			appKeychain:MutableKeychain, prefs:ModifiablePreferences, log:Logger) = {

		val init = mainClass.getMethod("init",
				classOf[KernelInterface], classOf[ModifiablePreferences], classOf[Logger])

		// Create a wrapper around the real kernel which saves keys to an app-specific keychain.
		val kernelWrapper = new KernelInterface() {
			override def save(bytes:ByteBuffer) =
				footlights save bytes tee { case f:data.File => appKeychain store f.link }

			override def open(name:String) = footlights openat (name split "/", root.dir)
			override def openDirectory(name:String) = root openMutableDirectory name

			override def open(name:URI) =
				appKeychain getLink { Fingerprint decode name } orElse {
					throw new AccessControlException("Unable to find key for '%s'" format name)
				} map footlights.open getOrElse {
					throw new java.io.FileNotFoundException("Unable to open '%s'" format name)
				}

			override def openLocalFile = footlights.openLocalFile tee { case f:data.File =>
				appKeychain store f.link
			}

			override def saveLocalFile(file:api.File) = footlights saveLocalFile file
		}

		new AppWrapper(init, name, kernelWrapper, prefs, log)
	}
}

/** Provides plugin [un]loading. */
trait ApplicationManagement extends Footlights {
	protected def keychain:MutableKeychain
	protected def loadedApps:mutable.Map[URI,AppWrapper]
	protected def appLoader:ClassLoader
	protected def prefs:ModifiablePreferences

	protected def readPrefs(filename:URI):Option[Map[String,String]]

	def runningApplications() = loadedApps.values toSeq

	override def loadApplication(uri:URI): Either[Exception,AppWrapper] =
		loadedApps get(uri) map Right.apply getOrElse {
			val appClass = loadAppClass(uri.toURL).right map {
				val appKeys = appKeychain(uri)
				val appPrefs = appPreferences(uri)
				val appRoot = appRootDir(uri, appKeys)
				val appLog = Logger getLogger uri.toString

				AppWrapper(_, uri, this, appRoot, appKeys, appPrefs, appLog)
			}
			appClass.right foreach { loadedApps put (uri, _) }

			appClass
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
		val map = mutable.Map() ++
			(prefs getString appKey map URI.create flatMap readPrefs getOrElse Map())

		ModifiableStorageEngine(map, Some(remember(appKey)))
	}

	/** Create a {@link Keychain} for an application which can save itself. */
	private def appKeychain(appName:URI) = {
		val appKey = "app.keychain." + appName

		prefs getString appKey map URI.create flatMap open map { case file:data.File =>
			Keychain parse file.getContents
		} orElse {
			Some(Keychain())
		} map { keys =>
			log finer "Loaded keychain for '%s': %s".format(appKey, keys)
			new MutableKeychain(keys, (k:Keychain) => remember(appKey)(k.getBytes))
		} getOrElse {
			throw new ProgrammerError("Failed to load or create app-specific Keychain")
		}
	}

	private def appRootDir(appName:URI, appKeychain:MutableKeychain) = {
		val appKey = "app.root." + appName

		prefs getString appKey map URI.create flatMap openDirectory tee {
			log fine "Loaded root directory for '%s': %s".format(appKey, _)
		} orElse
			Some(data.Directory()) map {
			new data.MutableDirectory(_, this, (d:data.Directory) => rememberDirectory(appKey)(d))
		} get
	}


	/** Load an application's main class from a given classpath. */
	private def loadAppClass(classpath:URL):Either[Exception,Class[_]] =
		(try { loadApplicationMethod.invoke(appLoader, classpath) }
		catch { case ex:Exception => Left(ex) }) match {
			case Right(c:Class[_]) => Right(c)
			case Left(ex:Exception) => Left(new AppStartupException(classpath.toURI, ex))
			case a:Any =>
				Left(new AppStartupException(classpath.toURI,
						new IllegalArgumentException(
								"%s returned '%s', rather than a Class or a Throwable" format (
										loadApplicationMethod.getName, a
									)
							)
					)
				)
		}


	private def rememberDirectory(prefKey:String)(dir:data.Directory) = {
		save(dir) tee {
			keychain store _.link } tee { d =>
			prefs set (prefKey, d.name.toString)
		} orElse {
			log warning "Failed to save root dir '%s'".format(prefKey)
			None
		}
	}

	/** Save some data, its {@link Link} (including symmetric key) and name. */
	private def remember(prefKey:String)(bytes:ByteBuffer) =
		save(bytes) map { case f:data.File => f } map { _.link } tee keychain.store map {
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
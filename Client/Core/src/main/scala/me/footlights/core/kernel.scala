/*
 * Copyright 2011-2012 Jonathan Anderson
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
import java.io.{FileInputStream,FileOutputStream}
import java.net.{URI,URL}
import java.security.{AccessController, AllPermission}
import java.util.logging.Level._
import java.util.logging.Logger

import javax.swing.JFileChooser

import scala.collection.{immutable,mutable}
import scala.collection.JavaConversions._

import me.footlights.api
import me.footlights.api.KernelInterface
import me.footlights.api.support.Either._
import me.footlights.core.data.store.CASClient

package me.footlights.core {

import apps.AppWrapper
import crypto.{Fingerprint,Keychain,MutableKeychain}
import data.store.{CASClient, DiskStore, Store}


/**
 * The Footlights kernel, composed of:
 *  - a filesystem
 *  - application management
 *  - a UI manager (for dispatching events to the web UI, swing UI, etc.)
 *
 * It is abstract, since it does not implement:
 *  - openLocalFile()
 *
 * These methods should be mixed in (e.g. "with {@link SwingDialogs}") on instantiation, as
 * should {@link KernelPrivilege} if running in a privilege-constrained environment.
 */
abstract class Kernel(
	protected val io: IO,
	protected val appLoader: ClassLoader,
	protected val prefs: FileBackedPreferences,
	protected val keychain: MutableKeychain,
	protected val loadedApps: mutable.HashMap[URI,AppWrapper],
	protected val uis: mutable.Set[UI],
	protected val cache: DiskStore)

	extends Footlights
		with data.store.Filesystem
		with apps.ApplicationManagement
		with Placeholders
		with UIManager
{
	private val resolver = Resolver(io, keychain)
	protected val store = CASClient(Preferences(prefs), resolver, Option(cache))    // TODO: don't wrap?

	/**
	 * Fetch the JAR file named by a {@link URI} (either directly by CAS hash-name or
	 * indirectly by an indirection {@link URL}) and store it locally as a conventional
	 * JAR file inside the OS' filesystem.
	 */
	override def localizeJar(uri:URI) = {
		val absoluteLink:Either[Exception,crypto.Link] =
			if (uri.isOpaque)
				keychain getLink Fingerprint.decode(uri) toRight {
					new Exception("Key for URI '%s' not stored in keychain" format uri)
				}
			else if (uri.getScheme != null) resolver.resolve(uri.toURL)
			else Left(new Exception("Tried to localize scheme-less URI '%s'" format uri.toString))

		absoluteLink flatMap {
			store fetch _ toRight(new Exception("%s not in store" format uri))
		} map { _.getInputStream } map {
			java.nio.channels.Channels.newChannel
		} map { in =>
			val tmpFile = java.io.File.createTempFile("footlights-dep", ".jar")
			tmpFile.setWritable(true)
			new FileOutputStream(tmpFile).getChannel.transferFrom(in, 0, MaxJarSize)
			tmpFile
		} map { new java.util.jar.JarFile(_) }
	}

	/** Read {@link Preferences} from a file. */
	protected def readPrefs(filename:URI) = {
		log info ("Reading preferences: %s" format filename)
		open(filename) fold (
			ex => None,
			_ match { case file:data.File => Some(Map() ++ Preferences.parse(file.getContents)) }
		)
	}

	private val log = Logger getLogger classOf[Kernel].getCanonicalName

	/** The largest JAR file which we are happy to open. */
	private val MaxJarSize = 1024 * 1024 * 1024
}


object Kernel {
	/** Create a Footlights kernel which uses a given ClassLoader to load applications. */
	def init(appLoader:ClassLoader) = {
		// This is the Footlights core, the security kernel; ensure that we can do anything.
		AccessController checkPermission { new AllPermission() }

		val fileBackedPrefs = FileBackedPreferences.loadFromDefaultLocation
		Flusher(fileBackedPrefs) start

		val prefs = Preferences.create(Option(fileBackedPrefs))

		val io = IO.direct

		val keychainFile = prefs getString { FileBackedPreferences.KEYCHAIN_KEY } map {
			new java.io.File(_) } get

		val keystore =
			if (keychainFile.exists) {
				try { Keychain.importKeyStore(new FileInputStream(keychainFile).getChannel) }
				catch {
					case e:Exception =>
						log.log(SEVERE, "Error loading keychain", e)
						Keychain()
				}
			} else Keychain()

		val keychain = new MutableKeychain(keystore, (k:Keychain) => security.Privilege.sudo { () =>
				val tmp = java.io.File.createTempFile("tmp-", "", keychainFile.getParentFile)
				k exportKeyStore { new FileOutputStream(tmp) getChannel }
				tmp renameTo keychainFile
			})

		val apps = new mutable.HashMap[URI,AppWrapper]
		val uis = new mutable.HashSet[UI]

		// Local disk cache for the network-based store.
		val cache =
			DiskStore.newBuilder
				.setPreferences(prefs)
				.setDefaultDirectory
				.build
		Flusher(cache) start

		new Kernel(io, appLoader, fileBackedPrefs, keychain, apps, uis, cache)
			with SwingPowerboxes
			with security.KernelPrivilege
	}

	private def getStoreLocation(
		key:String, prefs:Preferences, setupData:Option[Map[String,_]]) = {

		prefs getString("cas." + key) orElse {
			setupData.get.get(key) match { case Some(s:String) => Option(s); case _ => None }
		} map {
			new URL(_)
		}
	}

	private val log = Logger getLogger { classOf[Kernel].getCanonicalName }
}

}

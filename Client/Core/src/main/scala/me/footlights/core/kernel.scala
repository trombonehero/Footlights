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
import java.io.FileInputStream
import java.net.{URI,URL}
import java.security.{AccessController, AllPermission}
import java.util.logging.Level
import java.util.logging.Logger

import javax.swing.JFileChooser

import scala.collection.mutable.{HashMap,HashSet,LinkedList,Map,Set}
import scala.collection.immutable.{Map => ImmutableMap}

import me.footlights.api.KernelInterface
import me.footlights.core.data.store.BlockStoreClient

package me.footlights.core {

import apps.AppWrapper
import crypto.Keychain
import data.store.{BlockStoreClient, DiskStore, Store}


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
		i:IO, loader:ClassLoader, p:FileBackedPreferences, k:Keychain,
		apps:HashMap[URI,AppWrapper], u:Set[UI], s:Store)

	extends Footlights
		with Filesystem
		with Applications
		with UIManager
{
	val io = i
	val keychain = k
	val store = s
	val loadedApps = apps
	val appLoader = loader
	val prefs = p
	val uis = u
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

		val keychain = Keychain create
		val keychainFile = prefs getString { FileBackedPreferences.KEYCHAIN_KEY } map {
			new java.io.File(_) } filter { _.exists }

		if (keychainFile isDefined) {
			try keychain.importKeystoreFile(new FileInputStream(keychainFile get))
			catch {
				case e:Exception => log.log(Level.SEVERE, "Error loading keychain", e)
			}

			Flusher(keychain, keychainFile get) start
		}

		val apps = new HashMap[URI,AppWrapper]
		val uis = new HashSet[UI]

		// Local disk cache for the network-based store.
		val cache =
			DiskStore.newBuilder
				.setPreferences(prefs)
				.setDefaultDirectory
				.build
		Flusher(cache) start

		// Where are we storing data?
		val resolver = new Resolver(io, keychain)
		val setupData = prefs getString "init.setup" map { new URL(_) } flatMap { resolver.fetchJSON }
		val up = getStoreLocation("up", prefs, setupData)
		val down = getStoreLocation("down", prefs, setupData)

		val blockBuilder = BlockStoreClient.newBuilder setCache cache
		up foreach { blockBuilder.setUploadURL }
		down foreach { blockBuilder.setDownloadURL }
		val store = blockBuilder.build

		new Kernel(io, appLoader, fileBackedPrefs, keychain, apps, uis, store)
			with SwingPowerboxes
			with security.KernelPrivilege
	}

	private def getStoreLocation(
		key:String, prefs:Preferences, setupData:Option[ImmutableMap[String,_]]) = {

		prefs getString("blockstore." + key) orElse {
			setupData.get.get(key) match { case Some(s:String) => Option(s); case _ => None }
		} map {
			new URL(_)
		}
	}

	private val log = Logger getLogger { classOf[Kernel].getCanonicalName }
}

}

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
package me.footlights.android;

class GuiceModule2 extends com.google.inject.AbstractModule {
	override protected def configure = {
		try {
			// This is the Footlights core, the security kernel; ensure that we can do anything.
			java.security.AccessController checkPermission { new java.security.AllPermission }
			println("Got AllPermission!")
		} catch {
			case e:SecurityException =>
				println("We don't have the AllPermission!")
				e.printStackTrace
		}

		
		/*
		val fileBackedPrefs = FileBackedPreferences.loadFromDefaultLocation
		Flusher(fileBackedPrefs) start

		val prefs = Preferences.create(Option(fileBackedPrefs))

		val io = IO.direct

		val keychain = Keychain create
		val keychainFile = prefs getString { FileBackedPreferences.KEYCHAIN_KEY } map {
			new java.io.File(_) } get

		if (keychainFile.exists) {
			try { keychain.importKeystoreFile(new FileInputStream(keychainFile)) }
			catch {
				case e:Exception => log.log(Level.SEVERE, "Error loading keychain", e)
			}
		}
		Flusher(keychain, keychainFile) start

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
	   */
		/*
		ClassLoader loader = GuiceModule.class.getClassLoader();
		Class<?> kernelClass = Kernel.class;

		Kernel kernel = null;
		try { kernel = Kernel.init(loader); }
		catch (Exception e)
		{
			System.err.println("Error creating kernel: " + e);
			e.printStackTrace(System.err);
		}

		bind(Kernel.class).toInstance(kernel);
		*/
	}
}


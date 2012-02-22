/*
 * Copyright 2012 Jonathan Anderson
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
import java.io.{File,FileNotFoundException}
import java.lang.reflect.Method
import java.net.{MalformedURLException,URL}
import java.security.{AccessController,AllPermission,Policy,Security}
import java.util.{ArrayList,LinkedHashSet,List}
import java.util.logging.{Level,LogManager,Logger}

import scala.collection.JavaConversions._

import org.bouncycastle.jce.provider.BouncyCastleProvider


package me.footlights.boot {

case class UI(name:String, sourceDirectory:String, packageName:String, className:String)

case object WebUI extends UI("Local Web UI", "UI/Web", "me.footlights.ui.web", "WebUI")
case object SwingUI extends UI("Local Swing UI", "UI/Swing", "me.footlights.ui.swing", "SwingUI")


/**
 * Bootstraps the Footlights system.
 *
 * We need this to create our own {@link ClassLoader}s with full privilege and guarantee that no
 * other classes will be linked with such privilege.
 */
object Bootstrapper extends App {
	LogManager.getLogManager().readConfiguration(
		Bootstrapper.getClass.getResourceAsStream("logging.properties"));

	private val log = Logger getLogger Bootstrapper.getClass.getName

	val bootPath = "Bootstrap"
	val uis = WebUI ::
//		SwingUI ::
		Nil

	def pathExists(path:String) = new File(path).exists
	def toUrl(s:String) = new URL(s)

	val corePaths = "Core" :: (uis map { _.sourceDirectory })
	val coreClasspaths = (System.getProperty("java.class.path") split ":" map { path =>
		corePaths map { path replace (bootPath, _) } filter pathExists map { "file:" + _ } map toUrl
	}).flatten.toSet

	val classLoader = new FootlightsClassLoader(coreClasspaths)

	// Install crypto provider.
	Security addProvider new BouncyCastleProvider()

	// Set up our security policy and start enforcing it.
	Policy setPolicy new RestrictivePolicy()
	System setSecurityManager new SecurityManager()

	// Ensure that Bootstrapper, as the most privileged code around, can still do anything.
	AccessController checkPermission new AllPermission()

	// Load the Footlights class.
	val footlightsClass = classLoader.loadClass("me.footlights.core.Footlights")
	val coreClass = classLoader.loadClass("me.footlights.core.Kernel")

	val footlights:Object = try {
		coreClass getMethod("init", classOf[ClassLoader]) invoke(null, classLoader)
	} catch {
		case t:Throwable =>
			log log (Level.SEVERE, "Unable to start Footlights", t)
			System exit 1
			null
	}

	// Load the UI(s).
	val uiThreads = uis map { ui =>
		val className = ui.packageName + "." + ui.className;
		log info "Loading UI '%s' (%s/%s)".format(ui.name, ui.sourceDirectory, className)

		val init = classLoader loadClass className getMethod ("init", footlightsClass)

		init invoke (null, footlights) match {
			case r:Runnable => Some(new Thread(r, ui.name))
			case _ =>
				log severe "UI '%s' is not Runnable!".format(ui.name)
				None
		}
	} flatten

	uiThreads foreach { _.start }
	uiThreads foreach { thread =>
		log.fine("Waiting for " + thread.getName() + " to end");
		thread.join();
	}

	System exit 0
}

}


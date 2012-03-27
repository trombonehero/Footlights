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
import java.net.{MalformedURLException,URI,URL}
import java.security.{AccessController,AllPermission,Policy,Security}
import java.util.{ArrayList,LinkedHashSet,List}
import java.util.jar.JarFile
import java.util.logging.{Level,LogManager,Logger}

import scala.collection.JavaConversions._

import org.bouncycastle.jce.provider.BouncyCastleProvider


package me.footlights.boot {

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

	// Find Scala libraries in the Java "boot" classpath.
	val scalaPaths = System.getProperty("sun.boot.class.path") split ":" filter {
		_ contains "scala"
	}

	// Find Footlights code in the regular Java classpath.
	val classpaths = System.getProperty("java.class.path") split ":" filter { entry =>
		(entry contains "ootlights") && !(entry contains "test")
	}

	// Treat these two sets of classpaths as "core" (privileged) code.
	val coreClasspaths = (classpaths ++ scalaPaths) map { new File(_) } filter { _.exists } map {
		_.toURI.toURL
	}

	/** Uses the loaded {@link me.footlights.core.Kernel} to resolve a dependency URI. */
	def resolveDependencyJar(uri:URI):Option[JarFile] =
		if (localizeJar == null) None
		else localizeJar.invoke(footlights, uri) match {
			case Some(jar:JarFile) => Some(jar)
			case None =>
				log warning "Unable to resolve dependency %s".format(uri)
				None
			case a:Any =>
				log severe "Kernel method %s returned non-JAR: %s".format(footlights, a)
				None
		}

	val classLoader = new FootlightsClassLoader(coreClasspaths, resolveDependencyJar)

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
	val localizeJar = coreClass.getMethod("localizeJar", classOf[URI])

	val footlights:Object = try {
		coreClass getMethod("init", classOf[ClassLoader]) invoke(null, classLoader)
	} catch {
		case t:Throwable =>
			// If we fail to start Footlights, kill <b>all</b> threads.
			log log (Level.SEVERE, "Unable to start Footlights", t)
			System exit 1
			null               // required to satisfy type checker; System.exit does not return.
	}

	// Load the UI(s).
	log info "Searching %d classpaths for UIs...".format(coreClasspaths.length)
	val uiThreads = coreClasspaths map classLoader.open filter { loader =>
		(loader.right map { _.isUi }).right getOrElse { false }
	} map { _.right.get } map classLoader.loadUi map {
		_.right flatMap { _ initialize footlights }
	} map {
		case Right(ui) =>
			log info "Loaded UI %s".format(ui)
			Some(ui)

		case Left(exception) =>
			log log (Level.SEVERE, "Error loading UI", exception)
			None
	} flatten

	if (uiThreads.isEmpty) log severe { "No UIs found in %s" format coreClasspaths.toList }

	// Start all UIs running.
	uiThreads map { ui =>
		log info "Starting '%s' UI".format(ui.getName)
		ui start
	}

	uiThreads foreach { thread =>
		log.fine("Waiting for " + thread.getName() + " to end");
		thread.join();
	}

	System exit 0
}

}

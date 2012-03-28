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
import java.net.{URI,URL}
import java.util.jar.JarFile
import java.util.logging


package me.footlights.boot {

import ClasspathLoader.sudo


/** Loads "core" code (footlights.core.*, footlights.ui.*) from a known source */
class FootlightsClassLoader(
		classpaths:Iterable[URL], resolveDep:URI=>Option[JarFile]) extends ClassLoader {

	def open(privileged:Boolean = false)(classpath:URL) =
		ClasspathLoader.create(this, classpath, resolveDep, privileged)

	/**
	 * Load a privileged UI.
	 */
	def loadUi(loader:ClasspathLoader) =
		if (!loader.isUi) Left(new IllegalArgumentException("%s is not a UI" format loader))
		else
			loader.loadUi.right flatMap { ui =>
				val className = ui.getName
				val packageName = className.substring(0, className.lastIndexOf("."))

				ui.getMethods() find { _.getName equals "init" } map { init =>
					new UI(className, ui, init)
				} map Right.apply getOrElse {
					Left(new NoSuchMethodException("%s has no init() method" format className))
				}
			}

	/**
	 * Load an unprivileged application.
	 *
	 * Open the given classpath and load its main class, as specified in its manifest file
	 * (see {@link Classpath#mainClassName}).
	 */
	def loadApplication(classpath:URL): Either[Exception, Class[_]] =
		ClasspathLoader.create(this, classpath, resolveDep).right flatMap {
			_.loadMainClass.left map { new ClassNotFoundException(_) }
		}


	/** Load either a core Footlights class or a core library (e.g. Java or Scala) class. */
	override protected def loadClass(name:String, resolve:Boolean):Class[_] = synchronized {
		if (!ours(name)) getParent loadClass name
		else {
			// Option(findLoaded) getOrElse { findClass } makes the typechecker cry.
			val loaded = findLoadedClass(name)
			val c = if (loaded != null) loaded else findClass(name)
			if (resolve) resolveClass(c)
			c
		}
	}

	/** Find a core Footlights class. */
	override protected def findClass(className:String):Class[_] = {
		val result = findInCoreClasspaths(className)
		result.right getOrElse {
			throw new ClassNotFoundException(
					"Unable to find %s in any core classpath" format className,
					result.left.get)
		}
	}

	private[boot] def findInCoreClasspaths(className:String):Either[Exception,Class[_]] = {
		if (!ours(className)) Right(getParent loadClass className)
		else {
			val (success, errors) =
				coreClasspaths map { _ attemptLoadingClass className } partition { _.isRight }

			if (success.isEmpty)
				Left {
					new ClassNotFoundException("No %s in:\n%s\nErrors:\n%s" format (
							className,
							classpaths map { " - %s" format _ } reduce { _ + "\n" + _ },
							errors map { _.left get } reduce { _ + "\n" + _ })
					)
				}
			else Right(success.head.right.get)
		}
	}

	private def ours(name:String) = name startsWith "me.footlights"

	private val coreClasspaths = {
		for (url <- classpaths) yield {
			ClasspathLoader.create(this, url, resolveDep, withPrivilege = true) match {
				case Right(loader) => Some(loader)
				case Left(ex) =>
					log log (logging.Level.WARNING, "Error creating classloader " +  url, ex)
					None
			}
		}
	} flatten

	private var log = logging.Logger getLogger classOf[ClasspathLoader].getCanonicalName
	log fine { "Initialized with classpaths: %s" format classpaths }
}

/**
 * A privileged user interface.
 *
 * User interfaces are privileged because they accept user commands, bypassing applications'
 * {@link KernelInterface}.
 *
 * A UI must have a static "init" method which accepts a Footlights object as its only parameter.
 * This factory method should return a {@link import me.footlights.core.UI} object.
 */
class UI(val name:String, c:Class[_], init:java.lang.reflect.Method) {
	/** Initialize UI with the privileged interface of {@link Footlights}. */
	def initialize(footlights:Object) = init invoke (null, footlights) match {
		case r:Runnable => Right(new Thread(r, name))
		case _ => Left(new ClassCastException("UI '%s' is not Runnable" format name))
	}
}

}

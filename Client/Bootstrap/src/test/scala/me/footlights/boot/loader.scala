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
import java.net.URL


package me.footlights.boot {

/** Loads "core" code (footlights.core.*, footlights.ui.*) from a known source */
class FootlightsClassLoader(classpaths:Iterable[URL]) extends ClassLoader {
	/**
	 * Load an unprivileged application.
	 *
	 * Open the given classpath and load its main class, as specified in its manifest file
	 * (see {@link Classpath#mainClassName}).
	 */
	def loadApplication(classpath:URL) = ClasspathLoader.create(this, classpath).loadMainClass


	/** Load either a core Footlights class or a core library (e.g. Java or Scala) class. */
	override protected def loadClass(name:String, resolve:Boolean):Class[_] = synchronized {
		if (!(name startsWith "me.footlights")) getParent loadClass name
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
		// We must be loading a core Footlights class.
		if (!className.startsWith("me.footlights"))
			throw new IllegalArgumentException(
					classOf[FootlightsClassLoader].getSimpleName +
					".findClass() is only used directly for loading core Footlights classes, not" +
					className)

		// Do we already know what classpath to find the class in?
		val packageName = className.substring(0, className lastIndexOf '.')
		knownCorePackages get packageName map { _ loadClass className } getOrElse {
			// Lazily-evaluated stream of already-open classpath loaders.
			val known = {
				for ((corePackage, loader) <- knownCorePackages
					if packageName startsWith corePackage) yield
						loader findInClasspath className
			}

			// Exhaustive search of unopened core classpaths.
			val unknown = {
				for (url <- classpaths) yield {
					val loader = ClasspathLoader.create(this, url, Option.apply(packageName));
					loader findInClasspath className
				}
			}

			// Stream the two together, find the first success, note the correct loader and return.
			(known ++ unknown).flatten match {
				case c :: remainder =>
					val loader = c.getClassLoader match { case l:ClasspathLoader => l }
					knownCorePackages += (packageName -> loader)
					c
				case Nil =>
					throw new ClassNotFoundException("No %s in %s" format (className, classpaths))
			}
		}
	}

	private var knownCorePackages = Map[String, ClasspathLoader]()
}

}

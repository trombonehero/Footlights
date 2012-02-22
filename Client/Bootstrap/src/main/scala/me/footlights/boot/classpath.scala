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
import java.io.{File,FileInputStream,FilePermission,IOException}
import java.net.{MalformedURLException,URL}
import java.security.{AllPermission,Permission,PermissionCollection,Permissions}
import java.security.{CodeSigner,CodeSource,ProtectionDomain}
import java.util.jar.{JarEntry,JarFile,Manifest}
import java.util.logging.Logger

import collection.JavaConversions._

import com.google.common.collect.ImmutableList


package me.footlights.boot {

private case class Bytecode(bytes:Array[Byte], source:CodeSource)

/**
 * Loads classes and resources from a single classpath.
 *
 * @param parent            Parent ClassLoader.
 * @param classpath         The classpath (JAR or directory) that we are loading from
 * @param depPaths          External classpaths (JAR files).
 * @param permissions       Permissions that should be granted to loaded classes.
 * @param myBasePackage     The base package that we are responsible for. For instance,
 *                          if loading a plugin with packages com.foo.app and com.foo.support,
 *                          this parameter should be "com.foo". Only relevant for core classpaths.
 */
class ClasspathLoader(parent:ClassLoader, classpath:Classpath,
		depPaths:Iterable[URL], permissions:PermissionCollection, myBasePackage:Option[String])
	extends ClassLoader(parent) {
	/**
	 * Load a class, optionally short-circuiting the normal hierarchy.
	 *
	 * Unlike the usual model, we don't want the system ClassLoader to magically find classes
	 * for us: if it's a core class, we want the common definition, otherwise we want every
	 * sandboxed app to have its own version of everything (which can be thrown away easily).
	 */
	@throws(classOf[ClassNotFoundException])
	protected[boot] override def loadClass(name:String) = loadClass(name, false)

	@throws(classOf[ClassNotFoundException])
	protected override def loadClass(name:String, resolve:Boolean):Class[_] =
		attemptLoadingClass(name, resolve) getOrElse {
			throw new ClassNotFoundException(name + " not in " + classpaths)
		}


	override def getResource(name:String) = findResource(name)
	override def findResource(name:String) =
		classpath.url.toExternalForm match { case url => new URL(ensureFinalSlash(url) + name) }


	/** Load the "main" class, if one is specified in the manifest. */
	private[boot] def loadMainClass = classpath.mainClassName flatMap attemptLoadingClass orElse {
		throw new ClassNotFoundException(
			"%s does not specify a main Footlights class" format classpath.externalURL)
	}

	override def toString = {
		classOf[ClasspathLoader].getSimpleName + " { " +
			"base url = '" + classpath.url + "', " +
			"classpaths = " + classpaths + ", " +
			"permissions = " + permissions + ", " +
			"dependencies = " + dependencies + " " +
			"}"
	}


	/** Try to load a class. */
	private def attemptLoadingClass(name:String):Option[Class[_]] = attemptLoadingClass(name, false)
	private def attemptLoadingClass(name:String, resolve:Boolean = false):Option[Class[_]] =
		if (mustDeferToParent(name)) Some(getParent loadClass name)     // literal null is ok
		else
			findInClasspath(name) map { c =>
				if (resolve) resolveClass(c)
				c
			}

	/** Find a class within this classpath. */
	private[boot] def findInClasspath(name:String):Option[Class[_]] = synchronized {
		// Perhaps we've already loaded this class?
		loaded get name orElse {
			// If not, search through the places that might contain it.
			classpathsFor(name) flatMap { classpath =>
				sudo { () => classpath readClass name }
			} find { _ != None } map {
				case Bytecode(bytes,source) =>
					val domain = new ProtectionDomain(source, permissions)
					defineClass(name, bytes, 0, bytes.length, domain)
			} map { c =>
				loaded += (name -> c)
				c
			}
		}
	}


	/** Build a view of all of the {@link Classpath}s which might be able to load a given class. */
	private def classpathsFor(className:String) = synchronized {
		// Classpaths that we already know contain the relevant package.
		val knowns = List(classpath) ++ (
				classpaths.keys filter { className startsWith _ } flatMap { classpaths get _ }
			)

		// Full package name of the class to be loaded.
		val packageName = className.substring(0, className.lastIndexOf('.'))

		// Already-loaded dependencies which can load the desired class (lazily evaluated).
		val loadedDeps = (dependencies.values filter { _.isDefined } flatten).view filter {
			_ readClass className isDefined
		} map { cp =>
			classpaths += (packageName -> cp)
			cp
		}

		// As-yet-unloaded dependencies which might contain the class (lazily evaluated).
		val unloadedDeps = (dependencies filter { _._2.isEmpty } keys).view flatMap { url =>
			val cp = sudo { () => Classpath.open(url) }
			dependencies += (url -> cp)
			cp
		} filter {
			_.readClass(className).isDefined
		} map { cp =>
			classpaths += (packageName -> cp)
			cp
		}

		knowns ++ loadedDeps ++ unloadedDeps
	}

	/** Should we defer to the parent {@link ClassLoader} for this class? */
	private def mustDeferToParent(name:String) =
		myBasePackage flatMap { base =>
			if (isCorePackage(base)) Some(!(name startsWith base))
			else None
		} getOrElse isCorePackage(name)

	/** Is the given class in a core library package (which we shouldn't try to load)? */
	private def isCorePackage(className:String) =
		List("java.", "javax.", "scala.",
				"me.footlights.api", "me.footlights.core", "me.footlights.ui") exists className.startsWith

	/** Ensure that a path finishes with a '/'. */
	private def ensureFinalSlash(path:String) = path + (if (!(path endsWith"/")) '/' else "")


	/**
	 * Execute a function with JVM privilege (using {@link AccessController}).
	 *
	 * The name "sudo" is meant to be evocative of privilege in general;
	 * it does not refer specifically to system privilege as conferred by sudo(8).
	 *
	 * This is, unfortunately, duplicate code (as in "copy-and-paste"), but it should go away
	 * once we load the Scala libraries with the same privilege as core Java libraries.
	 */
	private def sudo[T](code:() => T):T =
		try java.security.AccessController.doPrivileged[T] {
			new java.security.PrivilegedExceptionAction[T]() { override def run:T = code() }
		}
		catch {
			case e:java.security.PrivilegedActionException => throw e getCause
		}

	/** Where we find our classes and resources (package name -> {@link Classpath}). */
	private var classpaths = Map[String,Classpath]()

	/**
	 * Classes we've already loaded.
	 *
	 * The default {@link #findLoadedClass} method provided by {@link ClassLoader} seems to block
	 * at very inconvenient times; let's just do it ourselves.
	 */
	private var loaded = Map[String,Class[_]]()

	/** External classpaths (which may not have been accessed yet). */
	private var dependencies:Map[URL,Option[Classpath]] = depPaths map { (_,Option[Classpath](null)) } toMap
}

object ClasspathLoader {
	/** Factory method for {@link ClasspathLoader}. */
	def create(parent:ClassLoader, path:URL, basePackage:Option[String]) = {
		// Adapt path URL. (TODO: do this elsewhere!)
		val completePath =
			if (!path.getProtocol().startsWith("jar:") && path.getPath().endsWith(".jar"))
				new URL("jar:" + path + "!/")
			else
				path

		// Open the classpath itself and make a note if we require dependencies.
		val classpath = Classpath.open(completePath).get
		if (classpath.dependencies.size > 0)
			log.info("Classpath '" + path + "' has dependencies: " + classpath.dependencies)

		// Only grant privileges to core Footlights code.
		val permissions = makeCollection {
			if (isPrivileged(basePackage)) new AllPermission
			else new FilePermission(path.toExternalForm, "read")
		}

		new ClasspathLoader(parent, classpath, classpath.dependencies, permissions, basePackage)
	}

	/** Convenience method for Java interop (Java doesn't understand default arguments). */
	def create(parent:ClassLoader, path:URL):ClasspathLoader = create(parent, path, None)


	/** Should code from the given package be privileged? */
	private def isPrivileged(name:Option[String]): Boolean = name map isPrivileged getOrElse false
	private def isPrivileged(name:String): Boolean =
		List("me.footlights.core", "me.footlights.ui") exists { name startsWith _ }

	/** Construct a read-only {@link PermissionCollection} with one {@link Permission} in it. */
	private def makeCollection(perm:Permission) = {
		val p = new Permissions
		p.add(perm)
		p.setReadOnly
		p
	}

	private val log = Logger getLogger { classOf[ClasspathLoader] getCanonicalName }
}



/** Holds bytecode from a Java class file */
private[boot]
abstract class Classpath(val url:URL) {
	def externalURL = url.toExternalForm
	def mainClassName = getManifestAttribute("Footlights-App")
	def dependencies =
		(getManifestAttribute("Class-Path") map { _ split ":" toList } flatten) filter isJar map {
			s => new URL("jar:file:" + s + "!/")
		}

	/** Read a class' bytecode. */
	def readClass(name:String): Option[Bytecode]

	/** Extract a global attribute from the classpath's manifest file. */
	protected def getManifestAttribute(key:String):Option[String]

	/** Does the given string name a JAR file? */
	private def isJar(s:String) = s endsWith ".jar"
}

private[boot]
object Classpath {
	def open(url:URL):Option[Classpath] = {
		url.getProtocol match {
			case "jar" => JARLoader.open(url)
			case "file" => FileLoader.open(url)
		}
	}
}


/** Loads classes from a local directory. */
private[boot]
class FileLoader(url:URL) extends Classpath(url) {
	/** Read a class from this filesystem hierarchy. */
	override def readClass(className:String) =
		open(className.split("\\.").toList, "class") map read map {
			Bytecode(_, new CodeSource(url, new Array[CodeSigner](0)))
		}

	override val toString = "FileLoader { %s }" format url

	override protected def getManifestAttribute(key:String) =
		manifest flatMap { m =>
			val target = "%s: " format key
			(m.indexOf(target) + target.length) match {
				case begin:Int if begin >= target.length =>
					m.indexOf('\n', begin) match {
						case end:Int if end > 0 => Option(m.substring(begin, end))
						case -1 => Option(m substring begin)
					}
				case _ => None
			}
		}

	/** Open a file within the current classpath. */
	private def open(path:List[String], extension:String) =
		new File((dirName :: path).reduceLeft(_ + pathSep + _) + "." + extension) match {
			case f:File if f.exists => Option(f)
			case _ => None
		}

	/**
	 * Read all bytes from a given file.
	 *
	 * This isn't at all efficient for large files, but the class files that I'm looking at are
	 * in the range [129 B, 52 kB].
	 */
	private def read(file:File) = {
		val bytes = new Array[Byte](file.length toInt)
		val stream = new FileInputStream(file)

		var offset = 0
		while (offset < bytes.length)
			offset += stream.read(bytes, offset, bytes.length - offset)
		stream.close

		bytes
	}

	/** The directory underneath which the classes are stored. */
	private val dirName = url.getFile
	private val dir = new java.io.File(dirName)
	if (!dir.exists) throw new java.io.FileNotFoundException("No such classpath '" + url + "'");

	/** Path component separator ('/' on UNIX, '\' on Windows). */
	private val pathSep = File.separatorChar.toString

	/** The manifest file, as a {@link String}. */
	private val manifest =
		open("META-INF" :: "MANIFEST" :: Nil, "MF") map read map { new String(_) }
}

private[boot]
object FileLoader {
	def open(url:URL) = Option(new FileLoader(url))
}


/** Loads classes from a single JAR file. */
private[boot]
class JARLoader(jar:JarFile, url:URL) extends Classpath(url) {
	protected val classPaths = jar.getManifest match {
		case null => throw new SecurityException("JAR file has no manifest (so it isn't signed)")
		case m:Manifest => m.getMainAttributes.getValue("Class-Path") match {
				case null => Nil
				case s:String => s split(" ") toList
			}
	}


	override def readClass(className:String) = {
		val classPath = className.replace('.', '/') + ".class"
		jar.entries find { _.getName.equals(classPath) } map { entry:JarEntry =>
			{
				val in = jar.getInputStream(entry)
				val bytes = new Array[Byte](in.available)
				in.read(bytes)

				val signers = entry.getCodeSigners
				if (signers == null) throw new SecurityException(entry.toString() + " not signed")
				val source = new CodeSource(url, signers)

				Bytecode(bytes, source)
			}
		}
	}

	override def getManifestAttribute(key:String) = jar.getManifest match {
		case null => throw new SecurityException("JAR file has no manifest (and isn't signed!)")
		case m:Manifest => m.getMainAttributes.getValue(key) match {
				case null => None
				case s:String => Option(s)
			}
	}
}

private[boot]
object JARLoader {
	def open(url:URL) =
		{
			try {
				makeJarUrl(url) openConnection match {
					case c:java.net.JarURLConnection => Option(c.getJarFile)
					case _ => None
				}
			} catch {
				case e:IOException => None
			}
		} map { new JARLoader(_, url) }

	private def makeJarUrl(url:URL) =
		if (url.toExternalForm.startsWith("jar:")) url else new URL("jar:" + url + "!/")
}

}

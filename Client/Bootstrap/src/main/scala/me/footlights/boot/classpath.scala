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
import java.net.{MalformedURLException,URI,URL}
import java.security.{AllPermission,Permission,PermissionCollection,Permissions}
import java.security.{CodeSigner,CodeSource,ProtectionDomain}
import java.util.jar.{JarEntry,JarFile,Manifest}
import java.util.logging.Logger

import collection.JavaConversions._


package me.footlights.boot {

import ClasspathLoader.sudo

private case class Bytecode(bytes:Array[Byte], source:CodeSource)

/**
 * Loads classes and resources from a single classpath.
 *
 * @param parent            Parent ClassLoader.
 * @param classpath         The classpath (JAR or directory) that we are loading from
 * @param permissions       Permissions that should be granted to loaded classes.
 * @param resolveDependency Resolves dependency URIs into locally-cached JAR files.
 * @param myBasePackage     The base package that we are responsible for. For instance,
 *                          if loading a plugin with packages com.foo.app and com.foo.support,
 *                          this parameter should be "com.foo". Only relevant for core classpaths.
 */
class ClasspathLoader(parent:FootlightsClassLoader, classpath:Classpath,
		permissions:PermissionCollection, resolveDependency:URI => Option[JarFile],
		privileged:Boolean = false)
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
	protected override def loadClass(name:String, resolve:Boolean):Class[_] = {
		attemptLoadingClass(name, resolve) fold ( ex =>
			if (!mayDeferToParent(name))
				throw new ClassNotFoundException("%s not found in %s" format (name, classpath))

			else
				parent findInCoreClasspaths name fold (
					ex => throw new ClassNotFoundException(
							"%s not found in %s or parent (%s)".format(
									name, classpath, ex.getMessage)),
					(c:Class[_]) => c
				),
			c => c
		)
	}


	override def getResource(name:String) = findResource(name)
	override def findResource(name:String) =
		classpath.url.toExternalForm match { case url => new URL(ensureFinalSlash(url) + name) }


	/** Load the "main" class, if one is specified in the manifest. */
	private[boot] def loadMainClass = classpath.mainClassName orElse {
		throw new ClassNotFoundException(
			"%s does not specify a main Footlights class" format classpath.externalURL)
	} map attemptLoadingClass get

	/** Does this classpath define a UI? */
	private[boot] def isUi = classpath.uiName.isDefined

	/** The name of the UI class specified by the manifest (if any). */
	private[boot] def loadUi = classpath.uiName map attemptLoadingClass getOrElse {
		Left(new IllegalArgumentException("Classpath %s is not a UI" format classpath))
	}

	override def toString = {
		classOf[ClasspathLoader].getSimpleName + " { " +
			"base url = '" + classpath.url + "', " +
			"classpaths = " + classpaths + ", " +
			"permissions = " + permissions + " " +
			"}"
	}


	/** Try to load a class. */
	private[boot] def attemptLoadingClass(name:String):Either[String,Class[_]] =
			attemptLoadingClass(name, false)

	private def attemptLoadingClass(name:String, resolve:Boolean = false) =
		if (mustDeferToParent(name)) Right(parent loadClass name)     // literal null is ok
		else
			findInClasspath(name).right map { c =>
				if (resolve) resolveClass(c)
				c
			}

	/** Find a class within this classpath. */
	private[boot] def findInClasspath(name:String):Either[String,Class[_]] = {
		// Perhaps we've already loaded this class?
		loaded get name map Right.apply getOrElse {
			// If not, search through the places that might contain it.
			classpaths flatMap { classpath =>
				sudo { () => classpath readClass name }
			} find { _ != None } map {
				case Bytecode(bytes,source) =>
					val domain = new ProtectionDomain(source, permissions)
					defineClass(name, bytes, 0, bytes.length, domain)
			} map { c =>
				loaded += (name -> c)
				c
			} map Right.apply getOrElse Left("%s not found in %s" format (name, classpaths))
		}
	}


	/** Should we defer to the parent {@link ClassLoader} for this class? */
	private def mustDeferToParent(name:String) =
		isLanguageRuntime(name) || (!privileged && isCorePackage(name))

	/** Is it <i>permissible</i> to defer to the parent {@link ClassLoader} for this class? */
	private def mayDeferToParent(name:String) = isCorePackage(name)

	/** Is the given class in the language runtime? */
	private def isLanguageRuntime(className:String) =
		List("java.", "javax.", "scala.", "sun.") exists className.startsWith

	/** Is the given class in a core library package (so be careful about loading it)? */
	private def isCorePackage(className:String) =
		List("org.apache",
				"me.footlights.api.", "me.footlights.boot.", "me.footlights.core.") exists {
			className.startsWith
		}

	/** Ensure that a path finishes with a '/'. */
	private def ensureFinalSlash(path:String) = path + (if (!(path endsWith"/")) '/' else "")

	/** Dependencies declared by the classpath's manifest. */
	private val dependencies =
			classpath.dependencies flatMap { resolveDependency(_) } map JARLoader.wrap toSeq

	/** All classpaths, including dependencies. */
	private val classpaths = classpath +: dependencies

	/**
	 * Classes we've already loaded.
	 *
	 * The default {@link #findLoadedClass} method provided by {@link ClassLoader} seems to block
	 * at very inconvenient times; let's just do it ourselves.
	 */
	private var loaded = Map[String,Class[_]]()
}

object ClasspathLoader {
	/** Factory method for {@link ClasspathLoader}. */
	def create(parent:FootlightsClassLoader, path:URL, resolveDependencyJar:URI => Option[JarFile],
			withPrivilege:Boolean = false):
			Either[Exception,ClasspathLoader] =
	{
		(Classpath open path).right map { classpath =>
			classpath.dependencies foreach { dep =>
				log fine ("Dependency for %s: %s => %s" format (path, dep, resolveDependencyJar(dep)))
			}

			// Only grant privileges to core Footlights code.
			val permissions = makeCollection {
				if (withPrivilege) new AllPermission
				else classpath.readPermission
			}

			new ClasspathLoader(parent, classpath, permissions, resolveDependencyJar, withPrivilege)
		}
	}


	/**
	 * Execute a function with JVM privilege (using {@link AccessController}).
	 *
	 * The name "sudo" is meant to be evocative of privilege in general;
	 * it does not refer specifically to system privilege as conferred by sudo(8).
	 *
	 * This is, unfortunately, duplicate code (as in "copy-and-paste"), but it should go away
	 * once we load the Scala libraries with the same privilege as core Java libraries.
	 */
	private[boot] def sudo[T](code:() => T):T =
		try java.security.AccessController.doPrivileged[T] {
			new java.security.PrivilegedExceptionAction[T]() { override def run:T = code() }
		}
		catch {
			case e:java.security.PrivilegedActionException => throw e getCause
		}


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
	def uiName = getManifestAttribute("Footlights-UI")
	def dependencies =
		(getManifestAttribute("Class-Path") map { _ split " " } flatten) map {
			new URI(_) } filter { _.getScheme != null }

	/** Read a class' bytecode. */
	def readClass(name:String): Option[Bytecode]

	/** Build a {@link FilePermission} representing the right to read this {@link Classpath}. */
	def readPermission: Permission

	/** Extract a global attribute from the classpath's manifest file. */
	protected def getManifestAttribute(key:String):Option[String]

	/** Does the given string name a JAR file? */
	private def isJar(s:String) = s endsWith ".jar"
}

private[boot]
object Classpath {
	def open(url:URL):Either[Exception,Classpath] = {
		try {
			url.getProtocol match {
				case "jar" => JARLoader open url
				case "file" =>
					if (url.getPath endsWith "jar") JARLoader open url
					else FileLoader open url
			}
		} catch {
			case e:Exception => Left(e)
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

	override val readPermission = {
		val dirname = url.getPath
		val path = if (dirname endsWith "/") dirname else (dirname + "/")
		new FilePermission(path + "-", "read")
	}

	override protected def getManifestAttribute(key:String) =
		manifest flatMap { m =>
			val target = "%s: " format key
			(m.indexOf(target) + target.length) match {
				case begin:Int if begin >= target.length =>
					m.indexOf('\n', begin) match {
						case end:Int if end > 0 => Option(m.substring(begin, end).trim)
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
	if (!sudo { () => dir.exists }) throw new java.io.FileNotFoundException("No such classpath '" + url + "'");

	/** Path component separator ('/' on UNIX, '\' on Windows). */
	private val pathSep = File.separatorChar.toString

	/** The manifest file, as a {@link String}. */
	private val manifest =
		open("META-INF" :: "MANIFEST" :: Nil, "MF") map read map { new String(_) }
}

private[boot]
object FileLoader {
	def open(url:URL) = Right(new FileLoader(url))
}


/** Loads classes from a single JAR file. */
private[boot]
class JARLoader(jar:JarFile, url:URL) extends Classpath(url) {
	protected val classPaths = jar.getManifest match {
		case null => throw new SecurityException(url + " has no manifest (so it isn't signed)")
		case m:Manifest => m.getMainAttributes.getValue("Class-Path") match {
				case null => Nil
				case s:String => s split(" ") toList
			}
	}

	private val fileName = url.getPath match { case s => s.substring(0, s.length - 2) }
	override val readPermission = new FilePermission(fileName, "read")

	override def readClass(className:String) = {
		val classPath = className.replace('.', '/') + ".class"
		jar.entries find { _.getName.equals(classPath) } map { entry:JarEntry =>
			{
				val bytes = {
					val in = jar.getInputStream(entry)
					val len = entry.getSize toInt
					val array = new Array[Byte](len)

					var (bytes, offset) = (1, 0)
					while (bytes > 0) {
						bytes = in.read(array, offset, len - offset)
						if (bytes > 0) offset += bytes
					}
					if (len != offset)
						throw new java.io.EOFException("Read %d B, expected %d" format (offset, len))
					array
				}

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

	override val toString = "JARLoader { %s } " format url
}

private[boot]
object JARLoader {
	def open(url:URL) = makeJarUrl(url) match {
		case url:URL =>
			(url openConnection match {
				case c:java.net.JarURLConnection => Right(c.getJarFile)
				case _ => Left(new Exception("Unknown error opening %s" format url))
			}).right map { new JARLoader(_, url) }
		}

	def wrap(jar:JarFile) = new JARLoader(jar, new URL("jar:file:" + jar.getName + "!/"))

	private def makeJarUrl(url:URL) =
		if (url.toExternalForm.startsWith("jar:")) url else new URL("jar:" + url + "!/")
}

}

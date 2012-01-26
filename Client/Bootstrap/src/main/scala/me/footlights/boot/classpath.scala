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
import java.io.{File,FileInputStream,IOException}
import java.net.{MalformedURLException,URL}
import java.security.{CodeSigner,CodeSource}
import java.util.jar.{JarEntry,JarFile,Manifest}

import collection.JavaConversions._


package me.footlights.boot {

/** Holds bytecode from a Java class file */
private[boot]
abstract class Classpath(url:URL) {
	def externalURL = url.toExternalForm
	def dependencies:List[URL]
	def readClass(name:String): Option[(Array[Byte],CodeSource)]
}

private[boot]
object Classpath {
	def open(url:URL, packageName:String):Option[Classpath] = {
		url.getProtocol match {
			case "jar" => JARLoader.open(url)
			case "file" => FileLoader.open(url)
		}
	}
}


/** Loads classes from a local directory. */
private[boot]
class FileLoader(url:URL) extends Classpath(url) {
	override def dependencies = Nil

	override def readClass(className:String) = {
		val file = open(className.split("\\.").toList, "class")
		val bytes = new Array[Byte](file map { _.length.toInt } getOrElse 0)

		file map { new FileInputStream(_) } foreach { stream => {
				var offset = 0
				while (offset < bytes.length)
					offset += stream.read(bytes, offset, bytes.length - offset)
				stream.close
			}
		}

		Some((bytes, new CodeSource(url, new Array[CodeSigner](0))))
	}

	/** Open a file within the current classpath. */
	private def open(path:List[String], extension:String) =
		new File((dirName :: path).reduceLeft(_ + pathSep + _) + "." + extension) match {
			case f:File if f.exists => Some(f)
			case _ => None
		}

	/** The directory underneath which the classes are stored. */
	private val dirName = url.getFile
	private val dir = new java.io.File(dirName)
	if (!dir.exists)
		throw new java.io.FileNotFoundException("No such classpath '" + url + "'");

	/** Path component separator ('/' on UNIX, '\' on Windows). */
	private val pathSep = File.separatorChar.toString
}

private[boot]
object FileLoader {
	def open(url:URL) = Some(new FileLoader(url))
}


/** Loads classes from a single JAR file. */
private[boot]
class JARLoader(jar:JarFile, url:URL) extends Classpath(url) {
	val dependencies = jar.getManifest match {
		case null => throw new SecurityException("JAR file has no manifest (so it isn't signed)")
		case m:Manifest => m.getMainAttributes.getValue("Class-Path") match {
			case null => Nil
			case s:String => List.fromArray(s.split(" ")) map { new URL(_) }
		}
	}

	/** Read a class' bytecode. */
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

				Some((bytes, source))
			}
		} getOrElse {
			throw new ClassNotFoundException("No class " + className + " in JAR file " + url)
		}
	}
}

private[boot]
object JARLoader {
	def open(url:URL) = Some(new JARLoader((new JAROpener).open(url), url))
}

}

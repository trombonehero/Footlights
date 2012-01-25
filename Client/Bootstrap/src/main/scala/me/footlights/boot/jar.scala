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
import java.io.{IOException,InputStream}
import java.net.{JarURLConnection,MalformedURLException,URL}
import java.security.{AccessController,CodeSource,PrivilegedAction}
import java.util.Enumeration;
import java.util.jar.{JarEntry,JarFile,Manifest}

import collection.JavaConversions._


package me.footlights.boot {

/** Loads classes from a single JAR file. */
private[boot] class JARLoader(jar:JarFile, url:URL) {
	def getJarFile = jar

	/** Read a class' bytecode. */
	def readBytecode(className:String) = {
		val classPath = className.replace('.', '/') + ".class"
		jar.entries find { _.getName.equals(classPath) } map { entry:JarEntry =>
			{
				val bytecode = new Bytecode
	
				val in = jar.getInputStream(entry)
				bytecode.raw = new Array[Byte](in.available)
				in.read(bytecode.raw)

				val signers = entry.getCodeSigners
				if (signers == null) throw new SecurityException(entry.toString() + " not signed")
				bytecode.source = new CodeSource(url, signers)

				bytecode
			}
		} getOrElse {
			throw new ClassNotFoundException("No class " + className + " in JAR file " + url)
		}
	}
}


private[boot] object JARLoader {
	def open(url:URL) = {
		if (!url.getProtocol().equals("jar"))
			throw new MalformedURLException("JAR URL does not start with 'jar:' '" + url + "'")

		val jar = (new JAROpener).open(url) match {
			case null => None
			case j:JarFile => Some(j)
		}

		jar map { _.getManifest } orElse {
			// Make sure the JAR file has a manifest; otherwise, it can't possibly be signed.
			throw new SecurityException("The jar file has no manifest (thus, cannot be signed)")
		} map {
			_.getEntries find { _._1.equals("Main-Class") } map { println(_) }
		}

		new JARLoader(jar.get, url)
	}
}

}
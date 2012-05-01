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
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.Channels

import scala.collection.JavaConversions._

import me.footlights.api
import me.footlights.api.support.Either._
import me.footlights.core.{Footlights,HasBytes,IO}
import me.footlights.core.crypto.{Fingerprint,Link,MutableKeychain}
import me.footlights.core.data
import me.footlights.core.data.{Directory,File}


package me.footlights.core.data.store {

/**
 * Provides the basics of a filesystem (opening and saving files), assuming that we have a
 * {@link Store} (for storing and retrieving data blocks) and a {@link Keychain} (for decrypting
 * encrypted data).
 */
trait Filesystem extends Footlights {
	protected def io:IO
	protected def keychain:MutableKeychain
	protected def prefs:api.ModifiablePreferences
	protected def store:Store

	override def open(link:Link):Either[Exception,File] = store fetch link match {
		case Some(x) => Right(x)
		case None => Left(new IOException("'%s' not found in block store" format link))
	}

	/** Open a file, named by its content, e.g. "urn:sha-256:0123456789abcdef01234...". */
	override def open(name:URI):Either[Exception,api.File] = {
		log fine { "open('%s')" format name }
		Option(name) map
			Fingerprint.decode flatMap
			keychain.getLink map
			open getOrElse
			Left(new IOException("Unable to open '%s' (unknown reason, sorry)" format name))
	}

	/** Open a {@link Directory} whose key is stored in our {@link Keychain}. */
	override def openDirectory(name:URI):Either[Exception,Directory] =
		Some(name) map
			Fingerprint.decode flatMap
			keychain.getLink map
			openDirectory getOrElse {
				Left(new NoSuchElementException("Key for '%s' not known" format name))
			}

	override def openDirectory(link:Link):Either[Exception,Directory] =
		store fetchDirectory link

	/** We cannot meaningfully create a {@link MutableDirectory} without a persistent name. */
	override def openDirectory(name:String) =
		Left(new java.io.IOException("Need a directory to open relative to"))

	/** Open a directory relative to a specified base. */
	override def openDirectory(names:Iterable[String], base:data.Directory) = {
		// Walk down through the directory hierarchy.
		var dir:Either[Exception,data.Directory] = Right(base)
		for (name <- names if !name.isEmpty) {
			dir = dir flatMap {
				_(name) filter {
				_.isDir } map
				Right.apply getOrElse {
					val path = names reduce { _ + "/" + _ }
					Left(new Exception("'%s' not a directory (in path '%s'" format (name, path))) }
			} map {
			_.link } flatMap
			store.fetchDirectory
		}

		dir
	}

	/** Open a file using a hierarchical name that recurses through directories. */
	override def open(name:String):Either[Exception,api.File] = {
		val names = name split "/"

		// The name must be of the form "urn:base-name/path/relative/to/base-name".
		openDirectory(URI create names.head) flatMap { base => openat(names.tail, base) }
	}

	/** Open a file using a hierarchical name relative to a base directory. */
	override def openat(names:Iterable[String], base:api.Directory) = base match {
		case d:data.MutableDirectory => openat(names, d.dir)
	}

	override def openat(names:Iterable[String], base:data.Directory) = {
		var dir = openDirectory(names.init, base)

		// Retrieve the file from the final directory.
		dir flatMap { _(names.last) filter { _.isFile } map { _.link } flatMap
			store.fetch map
			Right.apply getOrElse {
				val path = names reduce { _ + "/" + _ }
				Left(new java.io.IOException("No such file '%s'" format path))
			}
		}
	}

	/** Save a buffer of data to a {@link File}, whose name will be derived from the content. */
	override def save(data:ByteBuffer):Either[Exception,api.File] =
		save { File.newBuilder.setContent(data).freeze }

	/** Save a {@link File} that has already been generated to the {@link Store}. */
	def save(file:File) = {
		store store file.toSave
		log fine { "saved '%s'" format file }
		Right(file)
	}

	/** Save an immutable {@link Directory} to the {@link Store}. */
	def save(dir:Directory) = {
		store store dir.encrypted
		log fine { "saved dir '%s'" format dir }
		Right(dir)
	}

	/**
	 * Save data to a local {@link java.io.File}.
	 *
	 * We do what we can to make this as atomic an operation as possible: we write to a temporary
	 * file which is on the same filesystem as the target filename, then rename it to the target.
	 */
	override def saveLocal(f:File, filename:java.io.File) = try {
		val tmp = java.io.File.createTempFile("tmp-", "", filename.getParentFile)
		val out = io writer tmp

		out transferFrom (Channels newChannel f.getInputStream, 0, f.stat.length)
		out force true
		out close

		tmp renameTo filename
		Right(f)
	} catch { case ex:Exception => Left(ex) }

	/** List some of the files in the filesystem (not exhaustive!). */
	override def listFiles = store.listBlocks

	override protected def subsystemRoot(name:String) = {
		rootDirectory flatMap {
			_ openDirectory name } leftFlatMap { ex =>
			rootDirectory flatMap { _ mkdir name }
		} map {
			case d:data.MutableDirectory => d
		} getOrElse { ex =>
			new Error("Failed to open subsystem root '%s'" format name, ex)
		}
	}

	/** The root of our filesystem. */
	private lazy val rootDirectory = prefs.synchronized {
		prefs getString RootPrefKey map
			URI.create map
			openDirectory getOrElse {
			Right(data.Directory()) } map {
			new data.MutableDirectory(_, this, setNewRoot)
		}
	}

	/** Set a new root directory. */
	private def setNewRoot(dir:data.Directory) = prefs.synchronized {
		log info "Updated root: %s".format(dir)

		save(dir) map { _.link } tee
			keychain.store map {
			_.fingerprint.encode } foreach {
			prefs set (RootPrefKey, _)
		}
	}

	/** The key used to identify the global preference with the root name. */
	private val RootPrefKey = "root"

	private val log = java.util.logging.Logger getLogger classOf[Filesystem].getCanonicalName
}

class Stat(val name: Fingerprint, val length: Long) {
	override val toString = "File '%s' (%d B)" format (name, length)
}

object Stat {
	def apply(f:java.io.File) = new Stat(Fingerprint.decode(f.getName), f.length())
	def apply(name:Fingerprint, length:Long) = new Stat(name, length)
}

}

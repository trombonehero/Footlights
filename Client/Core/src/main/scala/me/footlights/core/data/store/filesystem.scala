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
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.Channels

import scala.collection.JavaConversions._

import me.footlights.core.{Footlights,HasBytes,IO}
import me.footlights.core.crypto.{Fingerprint,Link,MutableKeychain}
import me.footlights.core.data
import me.footlights.core.data.File

import me.footlights.api


package me.footlights.core.data.store {

/**
 * Provides the basics of a filesystem (opening and saving files), assuming that we have a
 * {@link Store} (for storing and retrieving data blocks) and a {@link Keychain} (for decrypting
 * encrypted data).
 */
trait Filesystem extends Footlights {
	protected def io:IO
	protected def keychain:MutableKeychain
	protected def store:Store

	override def open(link:Link):Option[File] = store fetch link

	/** Open a file, named by its content, e.g. "urn:sha-256:0123456789abcdef01234...". */
	override def open(name:URI):Option[api.File] = {
		log fine { "open('%s')" format name }
		Option(name) map
			Fingerprint.decode flatMap
			keychain.getLink flatMap
			open
	}

	/** Save a buffer of data to a {@link File}, whose name will be derived from the content. */
	override def save(data:ByteBuffer):Option[api.File] = {
			val f = File.newBuilder.setContent(data).freeze
			store store f.toSave
			log fine { "saved '%s'" format f }
			Some(f)
		}

	/**
	 * Save data to a local {@link java.io.File}.
	 *
	 * We do what we can to make this as atomic an operation as possible: we write to a temporary
	 * file which is on the same filesystem as the target filename, then rename it to the target.
	 */
	override def saveLocal(f:File, filename:java.io.File) = {
		val tmp = java.io.File.createTempFile("tmp-", "", filename.getParentFile)
		val out = io writer tmp

		out transferFrom (Channels newChannel f.getInputStream, 0, f.stat.length)
		out force true
		out close

		tmp renameTo filename
	}

	/** List some of the files in the filesystem (not exhaustive!). */
	override def listFiles = store.listBlocks

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

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
import java.nio.ByteBuffer

import scala.collection.JavaConversions._

import me.footlights.core.Footlights
import me.footlights.core.crypto.{Fingerprint,Keychain}
import me.footlights.core.data.File
import me.footlights.core.data.store.{LocalStore,Store}


package me.footlights.core.data.store {

/**
 * Provides the basics of a filesystem (opening and saving files), assuming that we have a
 * {@link Store} (for storing and retrieving data blocks) and a {@link Keychain} (for decrypting
 * encrypted data).
 */
trait Filesystem extends Footlights {
	protected def keychain:Keychain
	protected def store:Store

	/** Open a file, named by its content, e.g. "sha-256:0123456789abcdef01234...". */
	override def open(name:String):me.footlights.api.File =
		store fetch { keychain getLink { Fingerprint decode name } }

	/** Save a buffer of data to a {@link File}, whose name will be derived from the content. */
	override def save(data:ByteBuffer):me.footlights.api.File = {
			val f = File.newBuilder.setContent(data).freeze
			store.store(f.toSave())
			f
		}

	/** List some of the files in the filesystem (not exhaustive!). */
	override def listFiles = store.listBlocks
}

class Stat(val name: Fingerprint, val length: Long)

object Stat {
	def apply(f:java.io.File) = new Stat(Fingerprint.decode(f.getName), f.length())
	def apply(name:Fingerprint, length:Long) = new Stat(name, length)
}

}

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

package me.footlights.core {

import crypto.Fingerprint
import crypto.Keychain
import data.File
import data.store.Store


/**
 * Provides the basics of a filesystem (opening and saving files), assuming that we have a
 * {@link Store} (for storing and retrieving data blocks) and a {@link Keychain} (for decrypting
 * encrypted data).
 */
trait Filesystem {
	def keychain:Keychain
	def store:Store

	/** Open a file, named by its content, e.g. "sha-256:0123456789abcdef01234...". */
	def open(name:String) = store.fetch(keychain.getLink(Fingerprint.decode(name)))

	/** Save a buffer of data to a {@link File}, whose name will be derived from the content. */
	def save(data:ByteBuffer):_root_.me.footlights.api.File = {
			val f = File.newBuilder.setContent(data).freeze
			store.store(f.toSave())
			f
		}
}

}

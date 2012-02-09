/*
 * Copyright 2011-2012 Jonathan Anderson
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

import me.footlights.core.crypto.Fingerprint
import me.footlights.core.data.NoSuchBlockException


package me.footlights.core.data.store {

/** A block store in memory. */
class MemoryStore extends LocalStore(null) {
	val blocks = collection.mutable.Map[Fingerprint,ByteBuffer]()

	override def put(name:Fingerprint, bytes:ByteBuffer) = blocks.put(name, bytes)
	override def get(name:Fingerprint) =
		blocks.get(name) map { _.asReadOnlyBuffer } getOrElse {
			throw new NoSuchBlockException(this, name)
		}

	override def list = for ((name,bytes) <- blocks) yield Stat(name, bytes.remaining)

	/** Do nothing; {@link MemoryStore} always blocks. */
	override def flush = Unit
}

}

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

import java.io.{FileInputStream, IOException}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode

import java.util.logging.Logger

package me.footlights.core {

import security.Privilege

/** Provides efficient low-level I/O. */
object IO {
	/**
	 * Get bytes from a {@link java.io.File}, using whatever underlying mechanism will be most
	 * efficient for the file's size.
	 *
	 * TODO: stop explicitly invoking Privilege.sudo() here.
	 * Currently, we have to do this because the call stack looks like this:
	 *   * java.security.AccessControlContext.checkPermission
	 *   * [...]
	 *   * me.footlights.core.IO$$anonfun$read$1.apply(io.scala:34)
	 *   * at scala.Option.flatMap
	 *   * at me.footlights.core.IO$.read(io.scala:34)
	 *   * [...]
	 *   * at me.footlights.core.Privilege$.sudo(privilege.scala:34)
	 *
	 * So, between the sudo call and the access control check, there is a step through
	 * scala.Option.flatMap, which is unprivileged because it is neither a Footlights library
	 * nor a Java core library. The real solution involves one of:
	 *   * loading trusted Scala library code with the AllPermission (made somewhat risky by
	 *     the fact that the Scala library is not signed), or
	 *   * treating all code in the system classpath as fully-trusted and loading it with
	 *     the AllPermission (but there is some kind of incompatibility between Scala bytecode
	 *     and our ClassLoader).
	 *
	 * In the meantime, it's easy to throw in a Privilege.sudo().
	 */
	def read(f:Option[java.io.File]): Option[ByteBuffer] = f flatMap { file => Privilege.sudo { () =>
		if (!file.exists) None
		else {
			val channel = new FileInputStream(file).getChannel

			file.length match {
				// We only handle files with length < 2^31
				case n if n > Integer.MAX_VALUE => {
					log severe(
						"Tried to open " + file.length + " B file; we only handle up to "
						+ Integer.MAX_VALUE + " B")
					None
				}

				// If we're opening a large file, let the OS map it into memory.
				case n if n > 10000 => Some(channel.map(MapMode.READ_ONLY, 0, file.length()))

				// For smaller files, it can be more efficient to simply read the bytes out.
				case n:Long => {
					val buffer = ByteBuffer allocateDirect n.toInt
					channel read { buffer }
					buffer.flip
					Some(buffer)
				}
			}
		} }
	}

	private val log = Logger getLogger { IO getClass() getCanonicalName }
}

}

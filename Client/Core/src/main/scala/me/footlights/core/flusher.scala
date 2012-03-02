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
import java.io.{FileOutputStream,Flushable}
import java.nio.ByteBuffer

import java.util.logging.{Level,Logger}

package me.footlights.core {

import data.File


/** An object which periodically writes data to disk, the network, etc. */
class Flusher(name:String, flush:()=>Unit, wait:()=>Unit, log:java.util.logging.Logger)
	extends Thread("%s: %s" format (classOf[Flusher].getSimpleName, name))
{
	override def run() =
		try {
			while (true) {
				flush()
				wait()
			}
		} catch {
			case e:Exception => log.log(Level.WARNING, "Error flushing " + name, e)
		}
}

/** Companion object which helps us build new Flusher instances. */
object Flusher
{
	private val log = java.util.logging.Logger.getLogger(Flusher.getClass().getCanonicalName())

	def apply(f:Flushable) = new Flusher(
			name = f.getClass.getSimpleName,
			flush = f.flush _,
			wait = () => f.synchronized { f.wait },
			log = log)

	def apply(o:HasBytes, save:ByteBuffer => Any) = new Flusher(
			name = "%s => %s" format (o.getClass.getSimpleName, save),
			flush = () => save(o.getBytes),
			wait = () => o.synchronized { o.wait },
			log
		)

	def apply(o:HasBytes, filename:java.io.File) = new Flusher(
			name = o.getClass().getSimpleName() + " => " + filename.getCanonicalFile(),
			flush = () => {
				val tmp = java.io.File.createTempFile("tmp-", "", filename.getParentFile)

				val s = new FileOutputStream(tmp)
				s.getChannel.write(o.getBytes)
				s.close

				tmp renameTo filename
			},
			wait = () => o.synchronized { o.wait },
			log)
}

}

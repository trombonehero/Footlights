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

	def apply(name:String, flush:() => Unit, wait:() => Unit) = new Flusher(name, flush, wait, log)

	def apply(f:Flushable):Flusher = apply(
			name = f.getClass.getSimpleName,
			flush = f.flush _,
			wait = () => f.synchronized { f.wait }
		)

	def apply(target:HasBytes, save:ByteBuffer => Any):Flusher = apply(
			name = "%s => %s" format (target.getClass.getSimpleName, save),
			flush = () => save(target.getBytes),
			wait = () => target.synchronized { target.wait }
		)

	def apply(target:HasBytes, filename:java.io.File):Flusher = apply(
			name = target.getClass().getSimpleName() + " => " + filename.getCanonicalFile(),
			flush = () => {
				val tmp = java.io.File.createTempFile("tmp-", "", filename.getParentFile)

				val s = new FileOutputStream(tmp)
				s.getChannel.write(target.getBytes)
				s.close

				tmp renameTo filename
			},
			wait = () => target.synchronized { target.wait }
		)
}

}

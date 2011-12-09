import java.io.{FileOutputStream,Flushable}
import java.nio.ByteBuffer

import java.util.logging.{Level,Logger}

package me.footlights.core {

import data.File


/** An object which periodically writes data to disk, the network, etc. */
class Flusher(name:String, flush:()=>Unit, wait:()=>Unit, log:java.util.logging.Logger)
	extends Thread(name)
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
			f.getClass().getSimpleName(),
			() => f.flush(),
			() => f.synchronized { f.wait() },
			log)

	def apply(o:HasBytes, filename:java.io.File) = new Flusher(
			o.getClass().getSimpleName() + " => " + filename.getCanonicalFile(),
			() => {
				val s = new FileOutputStream(filename)
				s.getChannel().write(o.getBytes())
				s.close()
			},
			() => o.synchronized { o.wait() },
			log)
}

}

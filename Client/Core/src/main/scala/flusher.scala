package me.footlights.core

import data.File

import _root_.java.io.{FileOutputStream,Flushable}
import _root_.java.nio.ByteBuffer

import _root_.java.util.logging.{Level,Logger}


/** An object which periodically writes data to disk, the network, etc. */
class Flusher(flusher:()=>Any, timeout:Int, log:java.util.logging.Logger) extends Thread
{
	override def run() =
		try {
			while (true) {
				flusher()
				Thread.sleep(timeout)
			}
		} catch {
			case e:Exception => log.log(Level.WARNING, "Error flushing " + flusher, e)
		}
}

/** Companion object which helps us build new Flusher instances. */
object Flusher
{
	def newBuilder(f:Flushable) = new Builder(() => f.flush())
	def newBuilder(o:HasBytes, filename:java.io.File) =
		new Builder(() => new FileOutputStream(filename).getChannel().write(o.getBytes()))
}

/** Intermediate builder which accepts optional parameters. */
class Builder(flushFunction:()=>Any)
{
	var timeout:Int = 2000
	var log = java.util.logging.Logger.getLogger(Flusher.getClass().getCanonicalName())

	def build() = new Flusher(flushFunction, timeout, log)
}

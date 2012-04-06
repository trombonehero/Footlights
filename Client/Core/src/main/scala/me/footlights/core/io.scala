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
import java.io.{FileInputStream, FileOutputStream, IOException}
import java.net.URL
import java.nio.{Buffer,ByteBuffer}
import java.nio.channels.{ReadableByteChannel,WritableByteChannel}
import java.nio.channels.FileChannel.MapMode

import java.util.logging.Logger

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

import me.footlights.api.support.Either._


package me.footlights.core {

import data.File
import security.Privilege


/** Provides efficient low-level I/O. */
class IO(proxy:java.net.Proxy) {
	/**
	 * Get bytes from a {@link java.io.File}, using whatever underlying mechanism will be most
	 * efficient for the file's size.
	 */
	def read(file:java.io.File): Either[Exception,ByteBuffer] =
		if (!file.exists) Left(new IllegalArgumentException("%s does not exist" format file))
		else {
			val channel = new FileInputStream(file).getChannel

			file.length match {
				// We only handle files with length < 2^31
				case n if n > Integer.MAX_VALUE =>
					Left(new java.io.IOException(
							"Tried to open " + file.length + " B file; we only handle up to " +
							Integer.MAX_VALUE + " B"))

				// If we're opening a large file, let the OS map it into memory.
				case n if n > 10000 => Right(channel.map(MapMode.READ_ONLY, 0, file.length()))

				// For smaller files, it can be more efficient to simply read the bytes out.
				case n:Long =>
					val buffer = ByteBuffer allocateDirect n.toInt
					channel read { buffer }
					buffer.flip
					Right(buffer)
			}
		}

	def writer(file:java.io.File) = new FileOutputStream(file).getChannel


	def fetch(url: URL) = {
		// TODO: use URL.openConnection(Proxy)
		(try {
			Right(url openConnection proxy getInputStream) } catch { case ex:Exception =>
			Left(ex)
		}) map { in =>
			val data = new ListBuffer[Option[ByteBuffer]]
			while (in.available() > 0)
				// TODO: use some kind of NIO operation with channels and whatnot
				data += Option(ByteBuffer.allocate(4096)) map { b =>
					b.limit(in.read(b.array())) match { case bb:ByteBuffer => bb } }

			File.newBuilder().setContent(data flatten).freeze()
		}
	}

	private val log = Logger getLogger { this getClass() getCanonicalName }
}

object IO {
	def direct = new IO(java.net.Proxy.NO_PROXY)

	implicit def readable2rich(ch:ReadableByteChannel) = new RichReadableByteChannel(ch)
	implicit def writable2rich(ch:WritableByteChannel) = new RichWritableByteChannel(ch)
}

class RichReadableByteChannel(channel:ReadableByteChannel) {
	def readInt() = readInteger(4)
	def readShort() = readInteger(2) toShort
	def readByte() = readInteger(1) toByte

	def readInteger(byteCount:Int = 4) = {
		val bytes = new Array[Byte](byteCount)
		channel.read(ByteBuffer wrap bytes)
		(for (i <- 0 until byteCount) yield bytes(i) << 8 * (byteCount - i - 1)) reduce { _ + _ }
	}
}

class RichWritableByteChannel(channel:WritableByteChannel) {
	def << (x:ByteBuffer):RichWritableByteChannel = { channel write x; this }
	def << (x:Array[Byte]):RichWritableByteChannel = this << ByteBuffer.wrap(x)

	def << (x:Byte):RichWritableByteChannel  = writeInteger(x, 1)
	def << (x:Short):RichWritableByteChannel = writeInteger(x, 2)
	def << (x:Int):RichWritableByteChannel   = writeInteger(x, 4)

	def writeInteger(x:Int, byteCount:Int = 4) = {
		val bytes = {
			for (i <- 0 until byteCount)
				yield (x >> 8 * (byteCount - i - 1)) & 0xff
		} map { _.toByte }

		this << bytes.toArray
	}
}

}

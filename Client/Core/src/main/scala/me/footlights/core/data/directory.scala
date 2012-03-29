/*
 * Copyright 2012 Jonathan Anderson
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
import java.nio.channels.Channels

import scala.collection.JavaConversions._

import me.footlights.api
import me.footlights.core
import me.footlights.core.crypto.Link


package me.footlights.core.data {

class Entry(val name:String, val isDir:Boolean, val link:Link) {
	val isFile = !isDir

	override lazy val toString = name + (if (isDir) "/" else "")
	override def equals(a:Any) = {
		if (!a.isInstanceOf[Entry]) false
		else {
			val other = a.asInstanceOf[Entry]
			(other.name == name) && (other.isDir == isDir) && (other.link == link)
		}
	}
}

/**
 * Provides a name -> file mapping
 */
class Directory(private val entries:Map[String,Entry]) {
	import Directory._

	def + (mapping:Entry) = Directory { entries + (mapping.name -> mapping) }

	def ++ (dir:Directory) = Directory { entries ++ dir.entries }

	def apply(name:String) = entries get name

	lazy val link = encrypted.head.link

	override def equals(a:Any) = {
		if (!a.isInstanceOf[Directory]) false
		else a.asInstanceOf[Directory].entries == entries
	}

	override lazy val toString =
		if (entries.isEmpty) "Directory()"
		else "Directory(%s)" format (entries.values map { _.toString } reduce { _ + ", " + _ })

	lazy val encrypted:Iterable[EncryptedBlock] = {
		if (entries.size > Short.MaxValue)
			throw new FormatException("Cannot store %d directory entries" format entries.size)

		// Get byte-level representation of directory entries.
		val entryBytes = entries.values map { entry =>
			val name = entry.name.getBytes
			val size = entry.link.bytes + name.length

			(size, name, entry.isDir, entry.link)
		} toSeq


		// Build content blocks in reverse order, adding entries until they are full.
		var ciphertext = List[EncryptedBlock]()
		var blocks = List(
			Block.newBuilder setDesiredSize BlockSize addContent Directory.Terminator.toArray
		)

		def createNewBlock = {
			val oldHead = blocks.head
			val encrypted = oldHead.build.encrypt

			// The first link is to the next block.
			val b =
				Block.newBuilder
					.setDesiredSize(BlockSize)
					.addContent(Directory.Magic toArray)
					.addLink(encrypted.link)

			blocks ::= b
			ciphertext ::= encrypted

			b
		}

		// Walk through directory entries in reverse order, adding them to blocks.
		//
		// There's no reason we couldn't do another ordering, putting the most-shared things
		// at the end of the block chain and allowing sharing of partial directories.
		//
		// Future work.
		var current = blocks.head
		entryBytes.reverseIterator foreach { case Tuple4(size, name, isDir, link) =>
			if (size > current.remaining) current = createNewBlock

			if (name.length > (Short.MaxValue / 2))
				throw new FormatException("Name too long (%d B): '%s[...]'" format (
						name.length, new String(name, 0, 30)
					))
			val namelen = (name.length.toShort & 0x7FFF) + (if (isDir) 0x8000 else 0) toShort

			val entry = ByteBuffer allocate name.length + 2
			entry putShort namelen
			entry put name
			entry flip

			current addLink link
			current addContent entry
		}

		current.build.encrypt :: ciphertext
	}

	private val BlockSize = 4096   // TODO: something more generic
}

object Directory {
	def apply(entries:Map[String,Entry] = Map()) = new Directory(entries)

	def parse(blocks:Iterable[Block]): Either[Exception,Directory] = {
		var entries = blocks map { block =>
			val content = block.content
			val magic = new Array[Byte](Magic.length)
			content get magic

			val isTerminator = magic.toSeq match {
				case Terminator => true
				case Magic => false
				case other:Any =>
					return Left(new FormatException("Invalid directory magic '%s'" format other))
			}

			val links:Iterable[Link] = if (isTerminator) block.links else block.links.tail
			for (link <- links) yield {
				val namelen = content getShort
				val isDir = (namelen & 0x8000) == 0x8000
				val name = new Array[Byte](namelen & 0x7FFF)
				content get name

				new Entry(new String(name), isDir, link)
			}
		} reduce { _ ++ _ } map { entry => (entry.name -> entry) } toMap

		Right(new Directory(entries))
	}

	implicit def file2entry(x:(String,File)) = Entry(x._1, x._2)
	implicit def dir2entry(x:(String,Directory)) = Entry(x._1, x._2)

	/** Magic constant for a Footlights directory: vaguely "FOOTDIR" in hexaleet. */
	private val Magic = Seq(0xF0, 0x07, 0xD1, 0x12) map { _.toByte }

	/** Special magic for the last block in the directory chain. */
	private val Terminator = Seq(0xF0, 0x07, 0xD1, 0x13) map { _.toByte }
}

object Entry {
	def apply(name:String, file:File) = new Entry(name, isDir = false, file.link)
	def apply(name:String, dir:Directory) = new Entry(name, isDir = true, dir.link)
}

}

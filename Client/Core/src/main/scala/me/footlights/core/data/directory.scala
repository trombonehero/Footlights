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
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels

import scala.collection.JavaConversions._

import me.footlights.api
import me.footlights.api.support.Either._
import me.footlights.core
import me.footlights.core.crypto.Link


package me.footlights.core.data {

/**
 * A mapping from application-specified names to {@link File} and {@link Directory} objects.
 *
 * A {@link Directory} is mutable from an application perspective, but maps onto
 * immutable structures behind the {@link KernelInterface}.
 *
 * @param  notify     called when the {@link MutableDirectory} changes
 */
class MutableDirectory(var dir:Directory, footlights:core.Footlights, notify:Directory => Unit)
	extends api.Directory {

	override def snapshotName = dir.name
	override def entries = asJavaIterable { dir.entries map entry2entry }

	override def open(name:String) = footlights openat (name split "/", dir)

	def apply(name:String) = get(name)
	override def get(name:String) = dir(name) map entry2entry
	override def save(name:String, file:api.File) = file match {
		case f:File =>
			val e = Entry(name, f)
			dir += e
			notify(dir)
			Right(entry2entry(e))
	}
	override def save(name:String, bytes:ByteBuffer) =
		footlights save bytes flatMap { save(name, _) }

	override def save(name:String, d:api.Directory) = d match {
		case m:MutableDirectory => Right(entry2entry(save(name, m.dir)))
	}

	override def mkdir(name:String) = {
		get(name) map { e =>
			Left(new IllegalArgumentException("%s already exists: %s" format (name, e)))
		} getOrElse {
			save(name, MutableDirectory(footlights)(Directory())(save(name, _))) flatMap {
				_.directory
			}
		}
	}

	def openMutableDirectory(name:String): Either[Exception,api.Directory] = {
		var current:Either[Exception,api.Directory] = Right(this)
		for (component <- name split "/")
			current = current map { _ get component } flatMap {
				case Some(entry) => entry.directory
				case _ => Left(new IllegalArgumentException(
						"No such component '%s' in path '%s'" format (component, name)))
			}

		current
	}

	private def save(name:String, d:Directory) = {
		val e = Entry(name, d)
		dir += e
		notify(dir)
		e
	}

	private def entry2entry(e:Entry): api.Directory.Entry = new api.Directory.Entry {
		override val isDir = e.isDir
		override val name = e.name

		override lazy val directory = {
			if (e.isDir)
				footlights openDirectory e.link map {
					new MutableDirectory(_, footlights, notify = save(e.name, _))
				}
			else
				Left(new IOException("'%s' is not a directory" format name))
		}
		override lazy val file =
			if (e.isDir)
				Left(new IOException("'%s' is a directory, not a file" format name))
			else
				footlights open e.link

		override val toString = "('%s' => %s)" format (e.name, e.link)
	}
}

object MutableDirectory {
	def apply(footlights:core.Footlights)(dir:Directory)(notify:Directory => Unit) =
		new MutableDirectory(dir, footlights, notify)
}


/**
 * Provides a name -> file mapping
 */
class Directory(private val map:Map[String,Entry]) {
	import Directory._

	def + (mapping:Entry) = Directory { map + (mapping.name -> mapping) }

	def ++ (dir:Directory) = Directory { map ++ dir.map }

	def apply(name:String) = map get name

	def entries = map.values

	lazy val link = encrypted.head.link
	lazy val name = link.fingerprint.toURI

	override def equals(a:Any) = {
		if (!a.isInstanceOf[Directory]) false
		else a.asInstanceOf[Directory].map == map
	}

	override lazy val toString =
		if (map.isEmpty) "Directory()"
		else "Directory(%s)" format (map.values map { _.toString } reduce { _ + ", " + _ })

	lazy val encrypted:Iterable[EncryptedBlock] = {
		if (map.size > Short.MaxValue)
			throw new FormatException("Cannot store %d directory entries" format map.size)

		// Get byte-level representation of directory entries.
		val entryBytes = map.values map { entry =>
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
		if (blocks.isEmpty) Left(new IllegalArgumentException("A Directory must have blocks"))
		else {
			var terminated = false
			val entries = blocks map { block =>
				if (terminated) Nil
				else {
					val content = block.content
					val magic = new Array[Byte](Magic.length)
					content get magic

					val isTerminator = magic.toSeq match {
						case Terminator => true
						case Magic => false
						case other:Any =>
							return Left(new FormatException("Bad directory magic %s" format other))
					}
					terminated = isTerminator

					val links:Iterable[Link] = if (isTerminator) block.links else block.links.tail
					for (link <- links) yield {
						val namelen = content getShort
						val isDir = (namelen & 0x8000) == 0x8000
						val name = new Array[Byte](namelen & 0x7FFF)
						content get name

						(new String(name) -> new Entry(new String(name), isDir, link))
					}
				}
			} reduce { _ ++ _ } toMap

			Right(new Directory(entries))
		}
	}

	implicit def file2entry(x:(String,File)) = Entry(x._1, x._2)
	implicit def dir2entry(x:(String,Directory)) = Entry(x._1, x._2)

	/** Magic constant for a Footlights directory: vaguely "FOOTDIR" in hexaleet. */
	private val Magic = Seq(0xF0, 0x07, 0xD1, 0x12) map { _.toByte }

	/** Special magic for the last block in the directory chain. */
	private val Terminator = Seq(0xF0, 0x07, 0xD1, 0x13) map { _.toByte }
}


/** A directory entry (a file or a subdirectory). */
class Entry(val name:String, val isDir:Boolean, val link:Link) {
	val isFile = !isDir

	def dir = if (isDir) Some(link) else None
	def file = if (isDir) None else Some(link)

	override lazy val toString = name + (if (isDir) "/" else "")
	override def equals(a:Any) = {
		if (!a.isInstanceOf[Entry]) false
		else {
			val other = a.asInstanceOf[Entry]
			(other.name == name) && (other.isDir == isDir) && (other.link == link)
		}
	}
}

object Entry {
	def apply(name:String, file:File) = new Entry(name, isDir = false, file.link)
	def apply(name:String, dir:Directory) = new Entry(name, isDir = true, dir.link)
}

}

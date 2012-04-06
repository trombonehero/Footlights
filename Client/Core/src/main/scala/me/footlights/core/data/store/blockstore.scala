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
import java.io.{IOException,PrintWriter}
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.net.{HttpURLConnection,MalformedURLException,URL,URLConnection,URLEncoder}
import java.util.logging.Level._
import java.util.logging.Logger

import scala.actors.Future
import scala.actors.Futures.future
import scala.collection.JavaConversions._

import me.footlights.api.support.Tee._

import me.footlights.core.{Kernel,Preferences,Resolver}
import me.footlights.core.crypto.{Fingerprint,Link}
import me.footlights.core.data.{Block,Directory,EncryptedBlock,File}


package me.footlights.core.data.store {


/** Stores blocks of content. */
abstract class Store protected(cache:Option[LocalStore]) extends me.footlights.core.Flushable {
	@throws(classOf[java.io.IOException])
	protected def put(name:Fingerprint, bytes:ByteBuffer)

	protected def get(name:Fingerprint): Option[ByteBuffer]

	if (cache == null) throw new NullPointerException("null cache")
	if (cache.isDefined && cache.get == null) throw new NullPointerException("null cache")

	def store(block:Block): Unit = store(block.name, block.getBytes)
	def store(block:EncryptedBlock): Unit = store(block.name, block.ciphertext)
	def store(blocks:Iterable[EncryptedBlock]): Unit = {
		blocks foreach { store(_) }
		log finer "Stored %d blocks in %s".format(blocks.size, this)
	}

	def retrieve(name:Fingerprint):Option[ByteBuffer] =
		cache flatMap { _ retrieve name } orElse {
			this get name tee { bytes => cache foreach { _ store (name, bytes) } }
		}

	def retrieveCiphertext(link:Link) = retrieve(link.fingerprint) map {
		EncryptedBlock.newBuilder()
			.setLink(link)
			.setCiphertext(_)
			.build
	}

	/**
	 * Retrieve a list of {@link File} names which are known to exist in the {@link Store}.
	 *
	 * This is not guaranteed to be an exhaustive list; we only list files in the cache (if we
	 * have one), and even that isn't guaranteed to exhaustively list anything.
	 */
	def listBlocks:Iterable[Stat] = cache map { _.list } flatten

	/** Retrieve a stored (and encrypted) {@link File}. */
	def fetch(link:Link):Option[File] = {
		val encryptedHeader = retrieveCiphertext(link)
		encryptedHeader map { _.plaintext } map {
			_.links map retrieveCiphertext
		} filter { _.size > 0 } filter { _ forall { _.isDefined } } map { _.flatten } map {
			File.from(encryptedHeader.get, _)
		}
	}

	/** Retrieve a stored {@link Directory}. */
	def fetchDirectory(link:Link):Either[Exception,Directory] = {
		var next = Option(link)
		val plaintext = Iterator continually {
			next flatMap retrieveCiphertext map { _.plaintext } tee { block =>
				next = block.links match {
					case links if links.length > 0 => Option(links.head)
					case _ => None
				}
			}
		} takeWhile { _.isDefined } flatten

		if (plaintext.isEmpty)
			Left(new IllegalArgumentException("%s does not link to (valid) blocks" format link))
		else
			Directory parse plaintext.toIterable
	}

	/**
	 * If we have a cache, this method should not block for I/O. To ensure that the block has
	 * really been written to disk, the network, etc., call {@link #flush()}.
	 */
	private def store(name:Fingerprint, bytes:ByteBuffer): Unit =
		cache map { c =>
			c.store(name, bytes.asReadOnlyBuffer)
			synchronized {
				journal add name
				notify
			}
		} orElse {
			put(name, bytes.asReadOnlyBuffer)
			None
		}


	/** Wait until we have something to flush. */
	override def await = {
		Thread sleep flushTimeout_ms
		synchronized { while (journal.isEmpty) wait() }
	}

	/**
	 * Flush any stored blocks to disk/network, blocking until all I/O is complete.
	 */
	override def flush = synchronized {
		log finer "Flushing %d blocks in %s".format (journal.size, this)

		var unflushed = journal flatMap { name =>
			cache flatMap { _ retrieve name orElse {
					log severe "Cache inconsistency: %s not in cache %s".format(name, cache)
					None
				}
			} flatMap { bytes =>
				try { put(name, bytes); None }
				catch {
					case e:IOException =>
						log log (FINE, "Error flushing %s" format name, e)
						Option(name)
				}
			}
		}
		journal retain unflushed.contains

		if (journal.isEmpty) resetTimeout
		else {
			log info "%s unable to flush %d blocks".format(this, journal size)
			increaseTimeout
		}
	}

	private val InitialTimeout_ms = 500
	private val MaxTimeout_ms = 120000

	/** How long (in ms) to sleep between flush attempts. */
	private var flushTimeout_ms = InitialTimeout_ms
	private def increaseTimeout = flushTimeout_ms = math.min(2 * flushTimeout_ms, MaxTimeout_ms)
	private def resetTimeout = flushTimeout_ms = InitialTimeout_ms

	private val journal = collection.mutable.Set[Fingerprint]()
	private val log = java.util.logging.Logger getLogger classOf[Store].getCanonicalName
}



/** A block store in memory. */
class MemoryStore extends LocalStore {
	val blocks = collection.mutable.Map[Fingerprint,ByteBuffer]()

	override def put(name:Fingerprint, bytes:ByteBuffer) = blocks.put(name, bytes)
	override def get(name:Fingerprint) = blocks.get(name) map { _.asReadOnlyBuffer }

	override def list = for ((name,bytes) <- blocks) yield Stat(name, bytes.remaining)

	/** Do nothing; {@link MemoryStore} always blocks. */
	override def flush = Unit
}



/** A client for the Footlights Content-Addressible Store (CAS). */
class CASClient private[store](
		downloadUrl:Fingerprint => Option[URL], uploadUrl:() => Option[URL],
		uploadKey:Option[String], cache:Option[LocalStore])
	extends Store(cache) {

	override def toString() = "CASClient"


	override protected[store] def get(name:Fingerprint) = {
		downloadUrl(name) flatMap { _.openConnection match {
				case http:HttpURLConnection => Option(http)
				case _ =>
					log severe "Non-HTTP response for block " + name
					None
			}
		} filter validHttpResponse map { connection =>
			val buffer = ByteBuffer allocate connection.getContentLength
			val channel = Channels newChannel connection.getInputStream

			while (buffer.remaining > 0) channel read buffer

			buffer.flip
			buffer.asReadOnlyBuffer
		}
	}

	override protected[store] def put(name:Fingerprint, bytes:ByteBuffer) = {
		if (uploadKey.isEmpty) throw new IOException("No upload key set")

		val textFields = Map("AUTHENTICATOR" -> uploadKey.get, "EXPECTED_NAME" -> name.encode)
		val files = Map("upload" -> bytes)

		val CRLF = "\r\n"
		val boundary = "CASClientMIMEBoundary"
		val boundaryLine = "--" + boundary

		val c = (uploadUrl() getOrElse { throw new IOException("No upload URL") }).openConnection
		c setDoInput true
		c setDoOutput true
		c.setRequestProperty("Content-Type",
				"""multipart/form-data; boundary=%s""" format boundary)

		val out = c.getOutputStream
		val writer = new PrintWriter(out, true)

		textFields foreach { case (key, value) =>
			List(
				boundaryLine,
				"""Content-Disposition: form-data; name="%s"""" format key,
				"Content-Type: text/plain",
				"",
				value
			) foreach { writer append _ append CRLF }
			writer.flush
		}

		files foreach { case (name, bytes) =>
			List(
				boundaryLine,
				"""Content-Disposition: form-data; name="FILE_CONTENTS"; """ +
						"""filename="%s"""" format name,
				"Content-Type: application/octet-stream",
				"Content-Transfer-Encoding: binary",
//				"Content-Length: %d" format bytes.remaining,
				""
			) foreach { writer append _ append CRLF }
			writer.flush

			val copy = bytes.asReadOnlyBuffer
			val buffer = new Array[Byte](math.min(4096, copy.remaining))
			while (copy.hasRemaining) //binaryChannel write copy
			{
				val count = math.min(copy.remaining, buffer.length)
				copy.get(buffer, 0, count)
				out.write(buffer, 0, count)
			}
			out.flush

			writer append CRLF
			writer.flush
		}

		writer append boundaryLine append "--" append CRLF append CRLF
		writer.flush

		out.flush
		out.close

		new java.io.BufferedReader(new java.io.InputStreamReader(c.getInputStream)) readLine match {
			case s if s == name.encode => // The upload server returned the name that we expected.
			case other => throw new IOException("Bad name: " + other + " != " + name)
		}
	}


	private def validHttpResponse(http:HttpURLConnection) = {
		try {
			http.getResponseCode match {
				case 200 =>
					if (!http.getContentType.equals("application/octet-stream"))
						throw new IOException("Unknown mime-type: " + http.getContentType)
					true

				case other =>
					log severe "CAS error: HTTP code " + other
					false
			}
		} catch {
			case e:javax.net.ssl.SSLKeyException =>
				log.log(SEVERE, "SSL error connecting to CAS", e)
				false
		}
	}

	private val log = CASClient.log
}


object CASClient {
	def apply(prefs:Preferences, resolver:Resolver, cache:Option[LocalStore],
			uploadSecret:Option[String] = None) = {

		// Asynchronously retrieve CAS configuration data from a (locally-configurable) URL.
		val configData = future {
			val setupUrl = prefs getString PrefPrefix + "setup" orElse {
				Some("http://footlights.me/settings/cas.json")
			} map { new URL(_) }

			setupUrl foreach { log info "Retrieving CAS setup defaults from " + _ + "..." }
			setupUrl flatMap { resolver fetchJSON _ fold(
						ex => {
							log log (WARNING, "Unable to fetch JSON", ex)
							None
						},
						json => Some(json)
					) }
		}

		// A map of URLs for uploading and downloading CAS content (asynchronous, in case we're
		// not currently connected to the network).
		val urls = future {
			(List("uploadURL", "downloadURL") map { key =>
				prefs getString PrefPrefix + key orElse {
					configData() flatMap { _ get key } flatMap {
							case s:String => Option(s)
							case _ => None
						}
				} map { url =>
					log fine classOf[CASClient].getSimpleName + " " + key + ": " + url
					(key, new URL(url))
				}
			} flatten) toMap
		}

		def uploadUrl() = urls() get "uploadURL"
		def downloadUrl(name:Fingerprint) =
			urls() get "downloadURL" map { base =>
				new URL(base + "/" + URLEncoder.encode(name.encode, "utf-8")) }


		// The key used to upload content. If None, we can still use the CASClient for downloading.
		val uploadKey = uploadSecret orElse { prefs getString PrefPrefix + "secret" }

		val c = new CASClient(downloadUrl, uploadUrl, uploadKey, cache)
		me.footlights.core.Flusher(c).start
		c
	}

	private val log = Logger.getLogger(classOf[CASClient].getCanonicalName)

	/** The prefix for all CAS-related preferences. */
	private val PrefPrefix = "cas."
}

}

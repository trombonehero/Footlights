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

import me.footlights.core.{Kernel,Preferences,Resolver}
import me.footlights.core.crypto.Fingerprint
import me.footlights.core.data.{Block,EncryptedBlock,File,Link}


package me.footlights.core.data.store {


/** Stores blocks of content. */
abstract class Store protected(cache:Option[LocalStore]) extends java.io.Flushable {
	@throws(classOf[java.io.IOException])
	protected def put(name:Fingerprint, bytes:ByteBuffer)

	protected def get(name:Fingerprint): Option[ByteBuffer]

	if (cache == null) throw new NullPointerException("null cache")
	if (cache.isDefined && cache.get == null) throw new NullPointerException("null cache")

	def store(block:Block): Unit = store(block.name, block.getBytes)
	def store(block:EncryptedBlock): Unit = store(block.name, block.ciphertext)
	def store(blocks:Iterable[EncryptedBlock]): Unit = blocks foreach { store(_) }

	def retrieve(name:Fingerprint):Option[ByteBuffer] =
		cache flatMap { _ retrieve name } orElse { get(name) }

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
	def listBlocks:Collection[Stat] = cache map { _.list } flatten

	/** Retrieve a stored (and encrypted) {@link File}. */
	def fetch(link:Link):Option[File] = {
		val encryptedHeader = retrieveCiphertext(link)
		encryptedHeader map { _.plaintext } map {
			_.links map retrieveCiphertext
		} filter { _.size > 0 } filter { _ forall { _.isDefined } } map { _.flatten } map {
			File.from(encryptedHeader.get, _)
		}
	}

	/**
	 * If we have a cache, this method should not block for I/O. To ensure that the block has
	 * really been written to disk, the network, etc., call {@link #flush()}.
	 */
	private def store(name:Fingerprint, bytes:ByteBuffer): Unit = {
		if (cache.isEmpty) put(name, bytes.asReadOnlyBuffer)
		else cache.synchronized {
			cache map { _.store(name, bytes.asReadOnlyBuffer) }
			journal.synchronized { journal add name }
			synchronized { notify }
		}
	}


	/**
	 * Flush any stored blocks to disk/network, blocking until all I/O is complete.
	 */
	override def flush = if (cache != null) journal.synchronized {
		log finer "Flushing %d blocks in %s".format (journal.size, this)

		var unflushed = journal flatMap { name =>
			cache flatMap { _ get name orElse {
					log severe "Cache inconsistency: %s not in cache %s".format(name, cache)
					None
				}
			} flatMap { bytes =>
				try { put(name, bytes); None }
				catch { case e:IOException => Option(name) }
			}
		}
		journal retain unflushed.contains

		if (!journal.isEmpty) log info "Unable to flush %d blocks".format(journal size)
	}

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
		uploadKey:Option[String], resolver:Resolver, cache:Option[LocalStore])
	extends Store(cache) {

	override def toString() = "CASClient { }" format uploadUrl()

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
			val buffer = new Array[Byte](Math.min(4096, copy.remaining))
			while (copy.hasRemaining) //binaryChannel write copy
			{
				val count = Math.min(copy.remaining, buffer.length)
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

				case 404 => false
				case 410 => false
				case other =>
					log severe "Unknown CAS HTTP response code: " + other
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
			setupUrl flatMap { resolver fetchJSON }
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
					log info classOf[CASClient].getSimpleName + " " + key + ": " + url
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

		val c = new CASClient(downloadUrl, uploadUrl, uploadKey, resolver, cache)
		me.footlights.core.Flusher(c).start
		c
	}

	private val log = Logger.getLogger(classOf[CASClient].getCanonicalName)

	/** The prefix for all CAS-related preferences. */
	private val PrefPrefix = "cas."
}

}

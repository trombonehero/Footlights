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

import java.io.{ByteArrayOutputStream, IOException}
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.{Channels, ReadableByteChannel, WritableByteChannel}

import java.security.KeyStore
import java.security.cert.{Certificate,CertificateException}

import java.util.logging.Logger

import scala.collection.JavaConversions._

import me.footlights.core
import me.footlights.core.IO._
import me.footlights.core.data
import me.footlights.core.data.FormatException


package me.footlights.core.crypto {


/**
 * Stores cryptographic material.
 *
 * A Keychain can store two kinds of cryptographic material:
 * 1. symmetric keys, represented in {@link Link} objects which have authority to decrypt things and
 * 2. asymmetric keys, exposed as {@link SingingIdentity} objects have authority to can sign things.
 */
trait Keychain extends core.HasBytes {
	private[crypto] def links:Map[Fingerprint,Link]
	private[crypto] def serialized:ByteBuffer

	/** Concatenate with a link to an {@link EncryptedBlock}. */
	def + (link:Link): Keychain = new ImmutableKeychain(links + (link.fingerprint -> link))

	def ++ (k:Keychain): Keychain = new ImmutableKeychain(links ++ k.links)


	/** Get the {@link Link} which is capable of decrypting the named ciphertext. */
	def getLink(name:Fingerprint) = links get name

	/** Get a serialized representation which can be saved in the CAS. */
	override def getBytes = serialized.asReadOnlyBuffer 

	// Object overrides
	override def toString = "Keychain { %s }" format links
	override def equals(other:Any) = other match {
		case o:Keychain => links equals o.links
		case _ => false
	}


	/** Export to a Java {@link KeyStore} (e.g. the root {@link Keychain} on local disk). */
	private[core] def exportKeyStore(channel:WritableByteChannel,
			keystoreType:String = Keychain.defaultKeystoreType(),
			password:String = Keychain.getPassword()) = {
		log fine "Exporting to Java KeyStore..."

		val store = KeyStore.getInstance(keystoreType)
		store.load(null, password.toCharArray)    // completes KeyStore initialization (bad API!)

		// Protect keys with the same password as the keystore itself (common practice).
		val protection = new KeyStore.PasswordProtection(password.toCharArray)

		for ((fingerprint, link) <- links)
			store.setEntry(fingerprint.encode, new KeyStore.SecretKeyEntry(link.key.keySpec),
					protection)

		log fine { "Saved %d symmetric keys to Java KeyStore" format links.size }

		store.store(Channels newOutputStream channel, password toCharArray)
		store
	}

	private val log = Logger getLogger classOf[Keychain].getCanonicalName
}

/**
 * A mutable data structure which holds a variable, immutable {@link Keychain} and (optionally)
 * notifies an observer when that keychain changes.
 */
class MutableKeychain (private var keychain:Keychain, notify:Keychain => Unit = (_ => Unit))
		extends Keychain {

	def store(link:Link) = synchronized {
		keychain += link
		notify(keychain)
		this
	}

	private[crypto] def links() = keychain.links
	private[crypto] def serialized() = keychain.serialized
}


/** An immutable {@link Keychain} which can be concatenated with other things. */
class ImmutableKeychain private[crypto](
		private[crypto] val links:Map[Fingerprint,Link]) extends Keychain {

	private[crypto] lazy override val serialized =  {
		val out = new ByteArrayOutputStream()
		Keychain.serialize(this, Channels.newChannel(out))
		ByteBuffer wrap out.toByteArray
	}

	private val log = Logger getLogger classOf[Keychain].getCanonicalName
}


object Keychain {
	def apply(links:Iterable[(Fingerprint, Link)]): Keychain = apply(Map() ++ links)
	def apply(links:Map[Fingerprint,Link] = Map()): Keychain = new ImmutableKeychain(links)


	// TODO: return Either[Exception, Keychain]
	def parse(bytes:ByteBuffer): Either[Exception, Keychain] = {
		val magic = new Array[Byte](Magic.length)
		bytes get magic
		if (magic.deep != Magic.deep)
			Left(new FormatException("Invalid keychain magic: " + magic.array.toList))

		else try {
			val count = bytes getInt

			val links = for (i <- 0 until count) yield {
				val (namelen, keylen) = (bytes getInt, bytes getInt)
				val fingerprint = new Array[Byte](namelen)
				bytes get fingerprint

				val key = new Array[Byte](keylen)
				bytes get key

				val name = Fingerprint decode { new String(fingerprint) }
				val secret = SecretKey parse { new URI(new String(key)) }

				name -> (secret.createLinkBuilder setFingerprint name setKey secret build)
			}

			Right(apply(links))
		} catch {
			case ex:Exception => Left(ex)
		}
	}


	private[crypto] def serialize(keychain:Keychain, out:WritableByteChannel) = {
		out << Magic << keychain.links.size

		for ((fingerprint,link) <- keychain.links) {
			val name = fingerprint.encode
			val secret = link.key.toUri.toString

			out << name.length << secret.length
			out << name.getBytes << secret.getBytes
		}
	}


	def importKeyStore(channel:ReadableByteChannel,
			storeType:String = defaultKeystoreType(),
			password:String = getPassword()) = {

		val rawPassword = password.toCharArray

		val store = KeyStore getInstance storeType
		store.load(if (channel.isOpen) Channels newInputStream channel else null, rawPassword)
		log fine "Loaded %d KeyStore entries".format(store.size)

		val links = for (alias <- store.aliases) yield alias match {
			case Fingerprint(fingerprint) =>
				val secret = store.getKey(alias, rawPassword).asInstanceOf[javax.crypto.SecretKey]

				val key = SecretKey.newGenerator
					.setAlgorithm(secret.getAlgorithm)
					.setFingerprintAlgorithm(fingerprint.getAlgorithm.getAlgorithm)
					.setBytes(secret.getEncoded)
					.generate
				val link = key.createLinkBuilder
					.setFingerprint(fingerprint)
					.build

				Some(fingerprint -> link)

			case _ =>
				log warning "Invalid KeyStore entry '%s'".format(alias)
				None
		}

		apply(links.flatten toIterable)
	}


	/** Retrieve the keystore password from somewhere trustworthy (the user?). */
	private def getPassword() = "fubar"

	private lazy val prefs = core.Preferences.getDefaultPreferences
	private def defaultKeystoreType() = prefs getString "crypto.keystore.type" getOrElse {
		throw new core.ConfigurationError("No default keystore type configured")
	}

	/** Magic hexword: "Foot keys". */
	private val Magic = List(0xF0, 0x07, 0x6E, 0x75) map { _.toByte } toArray

	private val log = Logger getLogger Keychain.getClass.getCanonicalName
}

}

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
	private[crypto] def identities:Map[Fingerprint,SigningIdentity]
	private[crypto] def links:Map[Fingerprint,Link]
	private[crypto] def serialized:ByteBuffer

	/** Concatenate with a link to an {@link EncryptedBlock}. */
	def + (link:Link): Keychain =
		new ImmutableKeychain(identities, links + (link.fingerprint -> link))

	/** Concatenate with a {@link SigningIdentity}. */
	def + (name:Fingerprint, id:SigningIdentity): Keychain =
		new ImmutableKeychain(identities + (name -> id), links)

	def ++ (k:Keychain): Keychain =
		new ImmutableKeychain(identities ++ k.identities, links ++ k.links)


	/** Get the {@link Link} which is capable of decrypting the named ciphertext. */
	def getLink(name:Fingerprint) = links get name

	/**
	 * Get a private key.
	 *
	 * TODO: figure out an API that is less stupid (i.e. doesn't involve passing keys around).
	 */
	def getPrivateKey(name:Fingerprint) = identities get name 

	/** Get a serialized representation which can be saved in the CAS. */
	override def getBytes = serialized.asReadOnlyBuffer 

	// Object override
	override def toString = {
		"Keychain { identities: %s, links: %s }" format (identities, links)
	}
	override def equals(other:Any) = other match {
		case o:Keychain => (identities equals o.identities) && (links equals o.links)
		case _ => false
	}


	/** Export to a Java {@link KeyStore} (e.g. the root {@link Keychain} on local disk). */
	private[core] def exportKeyStore(channel:WritableByteChannel,
			keystoreType:String = Keychain.defaultKeystoreType(),
			password:String = Keychain.getPassword()) = {
		log fine "Exporting to Java KeyStore..."

		val store = KeyStore.getInstance(keystoreType)
		store.load(null, password.toCharArray)    // completes KeyStore initialization (bad API!)

		var privateEntries = for ((fingerprint, identity) <- identities) yield {
			val certChain = List(identity.getCertificate) toArray
			val entry = new KeyStore.PrivateKeyEntry(identity.getPrivateKey, certChain)
			(Keychain.PrivateKeyEntry, fingerprint, entry)
		}

		var symmetricEntries = for ((fingerprint, link) <- links) yield {
			val name = "%s:%s".format(Keychain.SymmetricKeyEntry, fingerprint.encode)
			val key = link.key.keySpec
			(Keychain.SymmetricKeyEntry, fingerprint, new KeyStore.SecretKeyEntry(key))
		}

		// Protect keys with the same password as the keystore itself (common practice).
		val protection = new KeyStore.PasswordProtection(password.toCharArray)
		for ((entryType, fingerprint, entry) <- (privateEntries ++ symmetricEntries))
			store.setEntry("%s:%s".format(entryType, fingerprint.encode), entry, protection)

		log fine "Saved %d private keys and %d secret keys to Java KeyStore".format(
				identities.size, links.size)

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

	def store(name:Fingerprint, id:SigningIdentity) = synchronized {
		keychain += (name, id)
		notify(keychain)
		this
	}

	private[crypto] def identities() = keychain.identities
	private[crypto] def links() = keychain.links
	private[crypto] def serialized() = keychain.serialized
}


/** An immutable {@link Keychain} which can be concatenated with other things. */
class ImmutableKeychain private[crypto](
		private[crypto] val identities:Map[Fingerprint,SigningIdentity],
		private[crypto] val links:Map[Fingerprint,Link]) extends Keychain {

	private[crypto] lazy override val serialized =  {
		val out = new ByteArrayOutputStream()
		Keychain.serialize(this, Channels.newChannel(out))
		ByteBuffer wrap out.toByteArray
	}

	private val log = Logger getLogger classOf[Keychain].getCanonicalName
}


object Keychain {
	type idMap = Map[Fingerprint,SigningIdentity]

	def apply(identities:idMap = Map(), links:Map[Fingerprint,Link] = Map()): Keychain =
		new ImmutableKeychain(identities, links)


	def parse(bytes:ByteBuffer) = {
		var identities = Map[Fingerprint,SigningIdentity]()
		var links = Map[Fingerprint,Link]()

		val magic = new Array[Byte](Magic.length)
		bytes get magic
		if (magic.deep != Magic.deep)
			throw new FormatException("Invalid keychain magic: " + magic.array.toList)

		val (privateSize, symmetricSize) = (bytes getInt, bytes getInt)
		for (i <- 0 until privateSize) {
			val (namelen, keylen) = (bytes getInt, bytes getInt)
			val (name, key) = (new Array[Byte](namelen), new Array[Byte](keylen))

			bytes get name
			bytes get key

			val fingerprint = Fingerprint decode new String(name)
			// TODO: SigningIdentity import/export
			/*
			val id = SigningIdentity parse key

			privateKeys += (fingerprint -> id)
			*/
		}

		for (i <- 0 until symmetricSize) {
			val (namelen, keylen) = (bytes getInt, bytes getInt)
			val fingerprint = new Array[Byte](namelen)
			bytes get fingerprint

			val key = new Array[Byte](keylen)
			bytes get key

			val name = Fingerprint decode { new String(fingerprint) }
			val secret = SecretKey parse { new URI(new String(key)) }

			links += name -> (secret.createLinkBuilder setFingerprint name setKey secret build)
		}

		apply(identities, links)
	}


	private[crypto] def serialize(keychain:Keychain, out:WritableByteChannel) = {
		out << Magic << keychain.identities.size << keychain.links.size

		for ((fingerprint,id) <- keychain.identities) {
			val name = fingerprint.encode
			val key = id.getPrivateKey.getEncoded

			out << name.length << key.length
			out << name.getBytes << key
		}

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
		var identities = Map[Fingerprint,SigningIdentity]()
		var links = Map[Fingerprint,Link]()

		val store = KeyStore getInstance storeType
		store.load(if (channel.isOpen) Channels newInputStream channel else null, rawPassword)
		log fine "Loaded %d KeyStore entries".format(store.size)

		for (alias <- store.aliases) alias match {
			case KeyStoreEntry(entryType, Fingerprint(fingerprint)) =>
				val keyEntry = store.getKey(alias, rawPassword)

				entryType match {
					case PrivateKeyEntry =>
						val key = keyEntry.asInstanceOf[java.security.PrivateKey]
						val cert = store getCertificate alias

						identities += (fingerprint -> SigningIdentity.wrap(key, cert))

					case SymmetricKeyEntry =>
						val secret = keyEntry.asInstanceOf[javax.crypto.SecretKey]
						val key = SecretKey.newGenerator
							.setAlgorithm(secret.getAlgorithm)
							.setFingerprintAlgorithm(fingerprint.getAlgorithm.getAlgorithm)
							.setBytes(secret.getEncoded)
							.generate
						val link = key.createLinkBuilder
							.setFingerprint(fingerprint)
							.build

						links += (fingerprint -> link)
				}

			case _ =>
				log warning "Invalid KeyStore entry '%s'".format(alias)
		}

		apply(identities, links)
	}


	/** Retrieve the keystore password from somewhere trustworthy (the user?). */
	private def getPassword() = "fubar"

	private lazy val prefs = core.Preferences.getDefaultPreferences
	private def defaultKeystoreType() = prefs getString "crypto.keystore.type" getOrElse {
		throw new core.ConfigurationError("No default keystore type configured")
	}

	/** Magic hexword: "Foot keys". */
	private val Magic = List(0xF0, 0x07, 0x6E, 0x75) map { _.toByte } toArray

	private val KeyStoreEntry = """(\S+?):(\S+)""".r
	private val PrivateKeyEntry = "private"
	private val SymmetricKeyEntry = "secret"

	private val log = Logger getLogger Keychain.getClass.getCanonicalName
}

}

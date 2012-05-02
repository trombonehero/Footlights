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
package me.footlights.core.crypto

import java.nio.ByteBuffer
import java.security.{Key,KeyFactory,MessageDigest,PublicKey}
import java.security.spec

import me.footlights.api
import me.footlights.api.support.Either._
import me.footlights.api.support.Pipeline._
import me.footlights.core
import me.footlights.core.crypto
import me.footlights.core.data


/** An identity which may have signed things. */
class Identity(val publicKey:PublicKey) extends core.HasBytes {
	def verify(x:(Fingerprint, ByteBuffer)): Boolean = x match { case (fingerprint, signature) =>
		verify(fingerprint, signature)
	}

	def verify(f:Fingerprint, signature:ByteBuffer): Boolean = {
		val s = new Array[Byte](signature.remaining)
		signature get s

		verify(f, s)
	}

	override def getBytes = {
		val algorithm = publicKey.getAlgorithm.getBytes
		val lengths = algorithm.length :: (fieldsToStore map { _.length }) map
			core.IO.int2bytes map {
			_.toArray
		}

		val header = magic.toArray :: lengths
		val body = algorithm :: fieldsToStore
		val complete = (header ++ body) map ByteBuffer.wrap

		val len = (0 /: complete) { _ + _.remaining }
		val buffer = ByteBuffer allocate len
		complete foreach buffer.put
		buffer.flip
		buffer.asReadOnlyBuffer
	}

	override val toString = "Identity { %s }" format { Fingerprint of encoded }
	override def equals(x:Any) = {
		if (!x.isInstanceOf[Identity]) false
		else {
			val other = x.asInstanceOf[Identity]
			other.encoded.toList == encoded.toList
		}
	}

	protected[crypto] lazy val fieldsToStore = List(encoded)

	protected[crypto] def signatureAlgorithm(hashAlgorithm:MessageDigest, key:Key) =
		java.security.Signature getInstance
			hashAlgorithm.getAlgorithm.replaceAll("-", "") + "with" + key.getAlgorithm

	private def verify(f:Fingerprint, signature:Array[Byte]) = {
		val verifier = signatureAlgorithm(f.getAlgorithm, publicKey)

		verifier initVerify publicKey
		verifier update f.copyBytes
		verifier verify signature
	}

	private lazy val encoded = publicKey.getEncoded
	protected def magic = Identity.Magic
}

object Identity {
	def apply(publicKey: PublicKey) = new Identity(publicKey)

	def parse(bytes:ByteBuffer): Either[Exception,Identity] = {
		val magic = new Array[Byte](Magic.length)
		bytes get magic

		parsers find { magic.toList == _._1 } map { case (magic, (fieldCount, decode)) =>
			val lengths = for (i <- 0 until fieldCount) yield bytes.getInt
			val algorithm :: fields = (
				for (len <- lengths) yield {
					val buffer = new Array[Byte](len)
					bytes get buffer
					buffer
				}).toList

			(new String(algorithm) | keyFactory | decode)(fields)
		} getOrElse {
			Left(new data.FormatException("Unknown magic '%s'" format magic.toList))
		}
	}

	def generate(
			publicKeyType:String = prefs getString "crypto.asym.algorithm" get,
			hashAlgorithm:String = prefs getString "crypto.hash.algorithm" get,
			keyLength:Int = prefs getInt "crypto.asym.keylen" get
			) = {

		val random = new java.security.SecureRandom    // TODO: specify random
		val generator = java.security.KeyPairGenerator getInstance publicKeyType
		generator.initialize(keyLength, random)

		SigningIdentity(generator generateKeyPair)
	}

	private def keyFactory(name:String) = KeyFactory getInstance name

	private def decodeIdentity(keyFactory:KeyFactory)(fields:List[Array[Byte]]) = {
		fields.toList match {
			case publicKey :: Nil =>
				decodePublic(keyFactory)(publicKey) | Identity.apply | Right.apply

			case l =>
				Left(new data.FormatException(
						"Expected one encoded field (public key); got %d" format l.length))
		}
	}

	private def decodeSigningIdentity(keyFactory:KeyFactory)(fields:List[Array[Byte]]) = {
		fields.toList match {
			case publicKey :: privateKey :: Nil =>
				val pub = decodePublic(keyFactory)(publicKey)
				val priv = decodePrivate(keyFactory)(privateKey)
				SigningIdentity(priv, pub) |
				Right.apply

			case l =>
				Left(new data.FormatException(
						"Expected two fields (public, private keys); got %d" format l.length))
		}
	}

	private def decodePublic(keyFactory:KeyFactory)(encoded:Array[Byte]) =
		encoded | (new spec.X509EncodedKeySpec(_)) | keyFactory.generatePublic

	private def decodePrivate(keyFactory:KeyFactory)(encoded:Array[Byte]) =
		encoded | (new spec.PKCS8EncodedKeySpec(_)) | keyFactory.generatePrivate

	private lazy val prefs = core.Preferences getDefaultPreferences

	/** Magic for {@link Identity}: FOOTID. */
	private val Magic = List(0xF0, 0x07, 0x1D, 0x00) map { _.toByte }

	private val parsers = Map(
		/** An {@link Identity} has two fields: an algorithm and a public key. */
		Identity.Magic -> (2, decodeIdentity _),

		/** A {@link SigningIdentity} has three fields: the two above plus a private key. */
		SigningIdentity.Magic -> (3, decodeSigningIdentity _)
	)
}

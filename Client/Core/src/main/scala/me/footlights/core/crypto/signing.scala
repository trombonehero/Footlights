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
import java.security.{Key,KeyPair,MessageDigest,PrivateKey,PublicKey}

import me.footlights.core


class Signature(val publicKey:PublicKey, val signingAlgorithm:String, rawBytes:ByteBuffer) {
	lazy val uri =
		new java.net.URI("urn",
			"%s:%s" format (
				signingAlgorithm,
				new String(new org.apache.commons.codec.binary.Base32().encode(rawBytes.array))
			), null)

	def verify(f:Fingerprint) = {
		val verifier = java.security.Signature getInstance signingAlgorithm

		verifier initVerify publicKey
		verifier update f.copyBytes
		verifier verify rawBytes.array
	}

	def copyBytes() = {
		val arr = new Array[Byte](rawBytes.remaining)
		rawBytes.asReadOnlyBuffer get arr
		arr
	}
}

object Signature {
	/** Wrap some data which purports to be a signature. */
	def apply(publicKey:PublicKey, hashAlgorithm:MessageDigest, rawSignature:ByteBuffer) =
		new Signature(publicKey, algorithmName(hashAlgorithm, publicKey), rawSignature)

	/** Create a signature by signing a Fingerprint. */
	def apply(fingerprint:Fingerprint, keys:KeyPair) = {
		val s = signatureAlgorithm(fingerprint.getAlgorithm, keys.getPrivate)

		s initSign keys.getPrivate
		s update fingerprint.copyBytes

		new Signature(keys.getPublic, s.getAlgorithm, ByteBuffer wrap s.sign)
	}

	protected[crypto] def algorithmName(hashAlgorithm:MessageDigest, key:Key) =
		hashAlgorithm.getAlgorithm.replaceAll("-", "") + "with" + key.getAlgorithm

	protected[crypto] def signatureAlgorithm(hashAlgorithm:MessageDigest, key:Key) =
		java.security.Signature getInstance algorithmName(hashAlgorithm, key)
}


/** An identity under our control, which can be used to sign things. */
class SigningIdentity(keyPair:KeyPair)
	extends Identity(keyPair.getPublic) with core.HasBytes {

	override val canSign = true

	private[core] def sign(fingerprint:Fingerprint) = Signature(fingerprint, keyPair)

	override def equals(a:Any) = a match {
		case s:SigningIdentity => super.equals(a) && java.util.Arrays.equals(s.encoded, encoded)
		case _ => false
	}

	protected[crypto] override lazy val fieldsToStore = List(publicKey.getEncoded, encoded)

	private[crypto] val privateKey = keyPair.getPrivate
	private lazy val encoded = privateKey.getEncoded

	override protected def magic = SigningIdentity.Magic
}


object SigningIdentity {
	def apply(keyPair:KeyPair) = new SigningIdentity(keyPair)
	def apply(privateKey:PrivateKey, publicKey:PublicKey): SigningIdentity =
		apply(new KeyPair(publicKey, privateKey))

	/** Magic: FOOTSI(gning)ID. */
	private[crypto] val Magic = List(0xF0, 0x07, 0x51, 0x1D) map { _ toByte }
}


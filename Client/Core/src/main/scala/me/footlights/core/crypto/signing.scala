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
import java.security.{KeyPair,NoSuchAlgorithmException,PrivateKey,PublicKey,Signature}

import me.footlights.core


/** An identity under our control, which can be used to sign things. */
class SigningIdentity(keyPair:KeyPair)
	extends Identity(keyPair.getPublic) with core.HasBytes {

	private[core] def sign(fingerprint:Fingerprint): ByteBuffer = {
		val s = signatureAlgorithm(fingerprint.getAlgorithm, privateKey)

		s initSign privateKey
		s update fingerprint.copyBytes
		ByteBuffer wrap s.sign asReadOnlyBuffer()
	}

	override lazy val getBytes = {
		val encoded = List(privateKey, publicKey) map { _ getEncoded } map ByteBuffer.wrap
		val lengths =
			(encoded map { _.remaining } map core.IO.int2bytes map { ByteBuffer wrap _.toArray })

		val parts =
			(ByteBuffer wrap SigningIdentity.Magic) ::
			lengths ++
			encoded

		val len = (0 /: parts) { _ + _.remaining }
		val buffer = ByteBuffer allocate len
		parts foreach buffer.put
		buffer
	}

	override def equals(a:Any) = a match {
		case s:SigningIdentity =>
			java.util.Arrays.equals(privateKey.getEncoded, s.privateKey.getEncoded)

		case _ => false
	}

	private[crypto] val privateKey = keyPair.getPrivate
}


object SigningIdentity {
	def apply(keyPair:KeyPair) = new SigningIdentity(keyPair)

	def wrap(privateKey:PrivateKey, cert:java.security.cert.Certificate) =
		new SigningIdentity(new KeyPair(cert.getPublicKey, privateKey))

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

	private lazy val prefs = core.Preferences getDefaultPreferences

	/** Magic: FOOTSI(gning)ID. */
	private val Magic = List(0xF0, 0x07, 0x51, 0x1D) map { _ toByte } toArray
}


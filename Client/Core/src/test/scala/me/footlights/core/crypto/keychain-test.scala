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
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito.when

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import me.footlights.api.support.Either._
import me.footlights.core.Preferences


package me.footlights.core.crypto {

@RunWith(classOf[JUnitRunner])
class KeychainTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {

	before {
		fingerprint = mock[Fingerprint]
		secretKey = mock[SecretKey]
		link = mock[Link]
		id = mock[SigningIdentity]
	}

	"A Keychain" - {
		"should be able to store private keys" in {
			val keychain = Keychain() + (fingerprint, id)
			keychain getPrivateKey fingerprint should equal (Some(id))
		}

		"should be able to store symmetric keys " in {
			when { link fingerprint } thenReturn fingerprint
			when { link key } thenReturn secretKey

			when { secretKey getFingerprint } thenReturn fingerprint

			val keychain = Keychain() + link
			keychain getLink fingerprint should equal (Some(link))
		}

		"should be able to export and import" in {
			Preferences.loadFromDefaultLocation
			val privateKey = SigningIdentity.newGenerator
				.setPrincipalName("Nobody")
				.setPublicKeyType("RSA")
				.generate

			val symmetricKey = SecretKey.newGenerator.generate
			val name = Fingerprint.newBuilder.setContent(Seq(1,2,3) map { _ toByte } toArray) build
			val link = symmetricKey.createLinkBuilder setFingerprint name build

			val keychain = Keychain() + link //+ (name, privateKey)
			val parsed = Keychain parse keychain.getBytes

			parsed should equal (Right(keychain))
			(parsed.get getLink name).get.key should equal (symmetricKey)
		}

		"should be able to save to a Java KeyStore" in {
			Preferences.loadFromDefaultLocation
			val privateKey = SigningIdentity.newGenerator
				.setPrincipalName("Nobody")
				.setPublicKeyType("RSA")
				.generate

			val symmetricKey = SecretKey.newGenerator.generate
			val name = Fingerprint.newBuilder.setContent(Seq(1,2,3) map { _ toByte } toArray) build
			val link = symmetricKey.createLinkBuilder setFingerprint name build

			val keychain = Keychain() + link + (name, privateKey)
			val out = new java.io.ByteArrayOutputStream
			keychain exportKeyStore { Channels newChannel out }

			val parsed = Keychain importKeyStore {
				Channels newChannel { new java.io.ByteArrayInputStream(out.toByteArray) }
			}

			parsed should equal (keychain)
			(parsed getLink name).get.key should equal (symmetricKey)
		}
	}

	var fingerprint:Fingerprint = _
	var secretKey:SecretKey = _
	var link:Link = _
	var id:SigningIdentity = _
/*
	@Test public void foo() throws Throwable
	{
		String algorithm = "RSA/ECB/OAEPwithSHA-512andMGF1padding";
		Cipher e = Cipher.getInstance(algorithm);
		e.init(Cipher.ENCRYPT_MODE, keychain.publicKey());

		ByteArrayOutputStream ciphertext = new ByteArrayOutputStream();
		OutputStream estream = new CipherOutputStream(ciphertext, e);

		final String testInput = "Hello, world!";

		Writer out = new OutputStreamWriter(estream);
		out.write(testInput);
		out.close();


		Cipher d = Cipher.getInstance(algorithm);
		d.init(Cipher.DECRYPT_MODE, keychain.privateKey.getPrivateKey());

		ByteArrayOutputStream plaintext = new ByteArrayOutputStream();
		OutputStream dstream = new CipherOutputStream(plaintext, d);


		dstream.write(ciphertext.toByteArray());
		dstream.close();

		assertEquals(testInput, new String(plaintext.toByteArray()));
	}
*/

}

}

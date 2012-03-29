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

import java.net.{URI,URL}
import java.nio.ByteBuffer

import scala.collection.JavaConversions._

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito.when

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import me.footlights.core.Preferences
import me.footlights.core.crypto.{Fingerprint,Keychain,Link,SecretKey}
import me.footlights.core.data.store._

package me.footlights.core.data {

@RunWith(classOf[JUnitRunner])
class RealFileIT extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {
	"The real block store" - {
		"should contain files." in {
			// Name and decryption key for a real file in the CAS (a library JAR file).
			val name = Fingerprint decode
				"urn:sha-256:BRNYBIBDXYMQRFF2G7R2UGDHTBT63JMXOIKASRW3K4MX75I2UEPQ===="
			val secretKey = SecretKey.parse(
					new URI("AES:d1bc705a152c3fc27f856a2f7394ec096d187a070034ea502e91d90a8e4b7f73"))
			val link = Link.newBuilder setFingerprint name setKey secretKey build

			val file = cache fetch link
			file should not be ('empty)
		}

		"should contain a Keychain" in {
			// Name and decryption key for a real keychain in the CAS.
			val name = Fingerprint decode
				"urn:sha-256:LCMCTDGMMUGXGYLNXVAZAJSLY5XGYNS2LMEYTDYHP7J5TJTQ5H5Q===="
			val secretKey = SecretKey.parse(
					new URI("AES:a63cc216ffb9017f1eaad652b6098d149b08c894516d537bd76ea37fe50180c9"))

			val file = cache fetch { Link.newBuilder setFingerprint name setKey secretKey build }
			file should not be ('empty)

			val keychain = Keychain parse file.get.getContents

			val keyName = Fingerprint decode
				"urn:sha-256:4S7RGYCAC4PI5QTA5B7CNFPYYUYERWW6YT5CO6W4JRSDDDVQVHFA===="

			keychain getLink keyName should not be ('empty)
		}

		"should give back real files with the same names that went in." in {
			val plaintext = Block.newBuilder
				.addContent(1 :: 2 :: 3 :: Nil map { _.toByte } toArray)
				.build

			val encrypted = plaintext.encrypt
			encrypted.ciphertext.remaining should equal (32)

			val file = File.newBuilder setBlocks plaintext :: Nil freeze

			file.name should equal (file.encryptedHeader.name.toURI)
			file.name should equal (
					Fingerprint.newBuilder
						.setContent(file.encryptedHeader.ciphertext)
						.build
						.toURI)

			val toSave = file.toSave
			cache store file.toSave

			var retrieved = cache fetch file.link
			retrieved.isDefined should equal (true)
			retrieved.get.name should equal (file.name)
			retrieved should equal (Some(file))
		}
	}

	private val prefs = Preferences.loadFromDefaultLocation
	private val cache = DiskStore.newBuilder
		.setPreferences(prefs)
		.setDefaultDirectory
		.build
}

}

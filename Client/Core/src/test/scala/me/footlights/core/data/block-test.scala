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

import java.nio.ByteBuffer
import java.security.NoSuchAlgorithmException

import scala.collection.JavaConversions._

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito.when

import org.powermock.api.mockito.PowerMockito

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import me.footlights.core.{Preferences,Util}
import me.footlights.core.crypto.{Fingerprint,Keychain,SecretKey}
import me.footlights.core.data.store._
import me.footlights.core.security


package me.footlights.core.data {

@RunWith(classOf[JUnitRunner])
class BlockTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {
	"We must be able to " - {
		"store and retrieve content." in {
			val data = List[Byte](1, 2, 3, 4) toArray
			val b = Block.newBuilder setContent ByteBuffer.wrap(data) build
			val copy = new Array[Byte](data.length)
			b.content().get(copy)

			copy should equal (data)
		}

		"parse bytes into a Block." in {
			val content = ByteBuffer allocate 16
			for (i <- 0 until content.capacity) content put i.toByte
			content.flip

			val data = ByteBuffer allocate 128
			Util setByteOrder data

			// Magic: spells 'FOOTDATA' approximately (plus some bin-text checking, like PNG).
			data put { List(0xF0, 0x07, 0xDA, 0x7A, '\r', '\n') map { _.toByte } toArray }
			data put { 7:Byte }                          // total size: 2^7 = 128
			data put { 1:Byte }                          // 1 link
			data putInt 16 + link.bytes                  // offset of user data

			data putInt content.capacity
			data put link.getBytes
			data put content

			val padding = ByteBuffer allocate data.remaining
			data put padding

			data flip


			val block = Block parse data

			block.bytes should equal (128)
			block.links.size should equal (1)
			block.links.get(0) should equal (link)

			block.content.limit should equal (content.capacity)
			for (i <- 0 until content.capacity) block.content.get(i) should equal (content.get(i))
		}

		"generate and parse blocks." in {
			var original = Block.newBuilder()
				.setContent(ByteBuffer wrap new Array[Byte](16))
				.addLink(link)
				.addLink(link)
				.build

			var parsed = Block parse original.getBytes

			parsed should equal (original)
		}

		"create blocks of multiple valid power-of-two sizes." in {
			(for (i <- 4 to 16) yield 1 << i) map { size =>
				(Block.newBuilder setDesiredSize size build).bytes should equal (size)
			}
		}

		"filter out non-power-of-two block sizes." in {
			List(15, 17, 31, 255, 257) map { size =>
				intercept[FormatException] { Block.newBuilder setDesiredSize size build }
			}
		}

		"name blocks correctly." in {
			val block = Block.newBuilder()
				.setContent { ByteBuffer wrap List[Byte](1,2,3).toArray }
				.build

			val encrypted = block.encrypt
			encrypted.name should equal (
					Fingerprint.newBuilder
						setAlgorithm encrypted.name.getAlgorithm.getAlgorithm
						setContent encrypted.ciphertext
						build)
		}
	}

	private val prefs = Preferences.loadFromDefaultLocation

	private val cryptoProvider = security.CryptoBackend.get

	private val name = Fingerprint.newBuilder setContent new Array[Byte](3) build
	private val secretKey = SecretKey parse new java.net.URI("JonCipher:0123456789")

	private val link = Link.newBuilder setFingerprint name setKey secretKey build
}

}

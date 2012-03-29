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

import me.footlights.core.crypto
import me.footlights.core.data.store._
import me.footlights.core.security


package me.footlights.core.data {

import Directory._

@RunWith(classOf[JUnitRunner])
class DirectoryTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {
	"A Directory should be able to " - {
		"store files." in {
			val d = Directory() + ("foo" -> file1) + ("bar" -> file2)

			for ((name, file) <- List("foo" -> file1, "bar" -> file2)) {
				val f = d(name)
				f should equal (Some(Entry(name, file)))
				f.get.link should equal (file.link)
			}
		}

		"store directories." in {
			val d = Directory() + ("foo" -> dir1) + ("bar" -> dir2)

			for ((name, dir) <- List("foo" -> dir1, "bar" -> dir2)) {
				val f = d(name)
				f should equal (Some(Entry(name, dir)))
				f.get.link should equal (dir.link)
			}
		}

		"store files and directories." in {
			val d = Directory() + ("foo" -> file1) + ("bar" -> dir1)

			d("foo") should equal (Some(Entry("foo", file1)))
			d("bar") should equal (Some(Entry("bar", dir1)))
		}

		"produce a parseable byte-level representation." in {
			val d = Directory() + ("foo" -> file1) + ("bar" -> dir1)
			val encrypted = d.encrypted
			encrypted.toList.length should be > (0)

			var link = encrypted.head.link
			val decrypted = encrypted map { e =>
				val plaintext = link decrypt e.ciphertext
				link = plaintext.links.head
				plaintext
			}

			val parsed = Directory parse decrypted
			parsed should equal (Right(d))
		}
	}

	private val link1 = Link.newBuilder
		.setFingerprint(crypto.Fingerprint.newBuilder setContent("link1".getBytes) build)
		.setKey(crypto.SecretKey.newGenerator.generate)
		.build

	private val dir1 = mock[Directory]
	private val file1 = mock[File]
	when { dir1.link } thenReturn link1
	when { file1.link } thenReturn link1

	private val link2 = Link.newBuilder
		.setFingerprint(crypto.Fingerprint.newBuilder setContent("link2".getBytes) build)
		.setKey(crypto.SecretKey.newGenerator.generate)
		.build

	private val dir2 = mock[Directory]
	private val file2 = mock[File]
	when { dir2.link } thenReturn link1
	when { file2.link } thenReturn link2
}

}

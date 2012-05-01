/*
 * Copyright 2011 Jonathan Anderson
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

import java.net.URL
import java.nio.ByteBuffer

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito.when

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import me.footlights.api.ajax.JSON
import me.footlights.api.ajax.JSON._

package me.footlights.core {

import crypto.{Fingerprint,Keychain,Link,SecretKey}
import data.File


@RunWith(classOf[JUnitRunner])
class ResolverTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {
	"A Resolver" - {
		"should resolve names with a specified key" in {
			val key = new Array[Byte](16); key(15) = 0x42
			val file = mockFile(f, Option("AES:00000000000000000000000000000042"))
			when(io.fetch(url)) thenReturn { file }

			resolver resolve url should equal(Right(
					Link.newBuilder()
						.setFingerprint(f)
						.setKey(SecretKey.newGenerator()
								.setAlgorithm("AES")
								.setBytes(key)
							.generate())
					.build())
				)
		}

		"should resolve names with no key specified" in {
			val file = mockFile(f)
			when { io fetch url } thenReturn file

			val l = mock[Link]
			when { keychain getLink f } thenReturn Some(l)

			resolver resolve url should equal (Right(l))
		}
	}

	val f = Fingerprint decode
			"urn:sha-256:JA6RDME6RLZPILMRNRBD5AX5SDGMSHAJ2WHZKBWYIPLHFPODHHRQ===="

	val url = new URL("http://127.0.0.1/foo/bar")

	val io = mock[IO]
	val keychain = mock[Keychain]

	var resolver:Resolver = _
	before { resolver = Resolver(io, keychain) }


	private def mockFile(fingerprint:Fingerprint, key:Option[String] = None) = {
		val json:JSON = Map("fingerprint" -> fingerprint.encode) ++ (key map { "key" -> _ } toMap)

		val f = mock[File]
		when { f copyContents } thenReturn { ByteBuffer wrap json.getBytes }
		Right(f)
	}
}

}

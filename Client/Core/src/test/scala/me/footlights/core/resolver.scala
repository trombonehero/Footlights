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

import org.scalatest.{BeforeAndAfter,Spec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import me.footlights.api.ajax.JSON
import me.footlights.core.crypto.{Fingerprint,Keychain,SecretKey}
import me.footlights.core.data.{File,Link}

package me.footlights.core {

@RunWith(classOf[JUnitRunner])
class ResolverTest extends Spec with BeforeAndAfter with MockitoSugar with ShouldMatchers {
	describe("A Resolver") {
		it("should resolve names with a specified key") {
			val key = new Array[Byte](16); key(15) = 0x42
			val file = mockFile(f, Some("AES:00000000000000000000000000000042"))
			when(io.fetch(url)) thenReturn { file }

			resolver.resolve(url) should equal(Some(
					Link.newBuilder()
						.setFingerprint(f)
						.setKey(SecretKey.newGenerator()
								.setAlgorithm("AES")
								.setBytes(key)
							.generate())
					.build())
				)
		}

		it("should resolve names with no key specified") {
			val file = mockFile(f)
			when(io.fetch(url)).thenReturn { file }

			val l = mock[Link]
			when(keychain.getLink(f)).thenReturn(l)

			resolver.resolve(url).get should equal(l)
		}
	}

	val f = Fingerprint.decode("sha-256:JA6RDME6RLZPILMRNRBD5AX5SDGMSHAJ2WHZKBWYIPLHFPODHHRQ====")
	val url = new URL("http://127.0.0.1/foo/bar")

	val io = mock[IO]
	val keychain = mock[Keychain]

	var resolver:Resolver = _
	before { resolver = new Resolver(io, keychain) }


	private def mockFile(fingerprint:Fingerprint, key:Option[String] = None):File = {
		val builder = JSON.newBuilder.put("fingerprint", fingerprint.encode)
		key foreach { builder.put("key", _) }
		val json = ByteBuffer wrap { builder.build.toString.getBytes }

		val f = mock[File]
		when { f getContents } thenReturn { json }
		f
	}
}

}

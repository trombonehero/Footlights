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
class IdentityTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {
	"An Identity " - {
		"should be be able to verify signatures." in {
			id verify (fingerprint -> signature) should equal (true)
		}

		"should be exportable and parseable." in {
			val bytes = id.getBytes
			val parsed = Identity parse bytes

			parsed should equal (Right(id))
		}
	}

	private val fingerprint = Fingerprint of { List(1,2,3,4) map { _ toByte } toArray }
	private val signer = SigningIdentity.generate()
	private val signature = signer sign fingerprint

	private val id = Identity(signer.publicKey)
}

}

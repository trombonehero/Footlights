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

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito.when

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import me.footlights.core.crypto.{Fingerprint,Keychain,SecretKey}
import me.footlights.core.data.{File,Link}
import me.footlights.core.data.store._

package me.footlights.core {

@RunWith(classOf[JUnitRunner])
class RealFileIT extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {
	"The real block store" - {
		"should contain a Keychain" in {
			val name = Fingerprint decode
				"sha-256:HVRFF7OBYMCOXZLGHMWGKKPSBB23PZYG7XO3M2XHNKAMKYG75W5Q===="
			val secretKey = SecretKey.parse(
					new URI("AES:3d6252fdc1c304ebe5663b2c6529f20875b7e706fdddb66ae76a80c560dfedbb"))
			val link = Link.newBuilder setFingerprint name setKey secretKey build

			val file = cache fetch link
			val keychain = Keychain.create importKeystoreFile file.get.getInputStream

			val keyName = Fingerprint decode
				"sha-256:YZDD7MJX2FMCVYG4LZVLQ6QRXWWNZ2R4J4MWRQR3F7TNF42A3UJQ===="

			keychain getLink keyName should not be null
		}
	}

	private val prefs = Preferences.loadFromDefaultLocation
	private val cache = DiskStore.newBuilder
		.setPreferences(prefs)
		.setDefaultDirectory
		.build
}

}

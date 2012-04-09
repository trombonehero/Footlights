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
import java.util.logging.Logger

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito.{when,verify}

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import me.footlights.api.{Application,File,KernelInterface,ModifiablePreferences}
import me.footlights.core.{Footlights}
import me.footlights.core.data
import me.footlights.core.crypto
import me.footlights.core.crypto.{Fingerprint,Keychain,MutableKeychain}


package me.footlights.core.apps {

@RunWith(classOf[JUnitRunner])
class AppsTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {
	var wrapper:AppWrapper = _
	before { wrapper = AppWrapper(TestApp.getClass, name, footlights, root) }

	"An AppWrapper " - {
		"should have the right name." in { wrapper.name should equal (name) }
		"should be able to save a file." in {
			val data = ByteBuffer allocate 16

			when { file link } thenReturn link
			when { footlights save data } thenReturn Right(file)

			wrapper.kernel save data should equal (Right(file))
		}
	}

	val name = new URI("some:name")
	val app = mock[Application]

	var file = mock[data.File]
	var link = crypto.Link.newBuilder
		.setFingerprint(crypto.Fingerprint of { List(1,2,3) map { _.toByte } toArray })
		.setKey(crypto.SecretKey.newGenerator.generate)
		.build

	var footlights = mock[Footlights]
	var root = mock[data.MutableDirectory]
	when { root(any()) } thenReturn None
	when { root get any() } thenReturn None

	/** We need to declare this explicitly because of the Evil Stuff we do on app init. */
	object TestApp {
		def init(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) = app
	}
}

}

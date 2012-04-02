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
import me.footlights.core.crypto.{Fingerprint,Keychain,MutableKeychain}


package me.footlights.core.apps {

@RunWith(classOf[JUnitRunner])
class AppsTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {
	var wrapper:AppWrapper = _
	before {
		appKeychain = new MutableKeychain(Keychain())
		wrapper = AppWrapper(TestApp.getClass, name, kernel, root, appKeychain, prefs, log)
	}

	"An AppWrapper " - {
		"should have the right name." in { wrapper.name should equal (name) }
		"should store keys in the app-specific keychain." in {
			val data = ByteBuffer allocate 16

			when { file link } thenReturn link
			when { link fingerprint } thenReturn fingerprint
			when { kernel save data } thenReturn Some(file)

			wrapper.kernel save data should equal (Some(file))
			appKeychain getLink fingerprint should equal (Some(link))
		}
	}

	val name = new URI("some:name")
	var appKeychain:MutableKeychain = _

	val app = mock[Application]

	var file = mock[data.File]
	var fingerprint = mock[Fingerprint]
	var kernel = mock[Footlights]
	var link = mock[me.footlights.core.crypto.Link]
	var log = mock[Logger]
	var prefs = mock[ModifiablePreferences]
	var root = mock[data.MutableDirectory]

	/** We need to declare this explicitly because of the Evil Stuff we do on app init. */
	object TestApp {
		def init(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) = app
	}
}

}

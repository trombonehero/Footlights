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

import java.io.IOException
import java.net.URL
import java.nio.ByteBuffer
import java.util.logging.Level._
import java.util.logging.Logger

import scala.collection.JavaConversions._

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito.when

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import me.footlights.core.Preferences
import me.footlights.core.crypto.Fingerprint
import me.footlights.core.data.Block
import me.footlights.core.data
import me.footlights.core.tags._


package me.footlights.core.data.store {


@RunWith(classOf[JUnitRunner])
class DiskStoreTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {

	private var store:DiskStore = _
	before {
		store = DiskStore.newBuilder()
			.createTemporaryDirectory()
			.setCache(None)
			.build
	}

	"A DiskStore" - {
		"should be able to store plaintext" in {
			store store b1
			Block parse { store retrieve b1.name get } should equal(b1)
		}

		"should be able to fetch files" in {
			val blocks = List(b1, b2) map { _.getBytes }
			val file = data.File.newBuilder setContent blocks freeze

			store store file.toSave
			store fetch file.link should equal (Some(file))
		}

		"should be able to deal with realistic-sized files" in {
			val blocks = List(b1, b2, bigBlock) map { _.getBytes }
			val file = data.File.newBuilder setContent blocks freeze

			store store file.toSave
			store fetch file.link should equal (Some(file))
		}
	}

	private val b1 = Block.newBuilder()
		.addContent(List[Byte](1, 2, 3, 4).toArray)
		.build

	private val b2 = Block.newBuilder()
		.addContent(List[Byte](5, 6, 7, 8).toArray)
		.build

	private lazy val bigBlock = Block.newBuilder()
		.addContent(ByteBuffer allocate 32000)
		.build
}

}

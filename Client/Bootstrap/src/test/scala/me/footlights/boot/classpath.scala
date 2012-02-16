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
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.security.{AllPermission,Permission,Permissions,PermissionCollection}

import scala.collection.JavaConversions._

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito._

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers._
import org.scalatest.mock.MockitoSugar

package me.footlights.boot {

/** Integration tests for {@link ClasspathLoader}. */
@RunWith(classOf[JUnitRunner])
class ClasspathLoaderTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {

	var loader:ClasspathLoader = _
	before { loader = new ClasspathLoader(parent, path, basePackage, deps, perms) }

	val parent = mock[ClassLoader]
	val path = mock[Classpath]
	val basePackage = "com.example.foo"
	val deps = List[URL]()
	val perms = new Permissions

	// Classes loaded by the ClassLoader under test.
	var core:Class[_] = _
	var good:Class[_] = _
	var evil:Class[_] = _

	"A ClasspathLoader " - {
		"should always defer to the parent for core classes" in {
			val name = "me.footlights.core.Something"
			val loaded = loader loadClass name

			verify(parent, times(1)) loadClass name
			loaded should be (null)
		}

		"should never refer to the parent for non-core classes" in {
			val name = "me.footlights.demo.Anything"

			intercept[ClassNotFoundException] { loader loadClass name }

			verify(parent, times(0)) loadClass name
		}
	}
}

}

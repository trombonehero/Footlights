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
import java.security.{AllPermission,Permission,PermissionCollection}

import scala.collection.JavaConversions._

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito._

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers._
import org.scalatest.mock.MockitoSugar

package me.footlights.boot {

/** Integration tests for {@link FootlightsClassLoader}. */
@RunWith(classOf[JUnitRunner])
class ClassLoadingIT extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {

	var loader:FootlightsClassLoader = _

	// Classes loaded by the ClassLoader under test.
	var core:Class[_] = _
	var good:Class[_] = _
	var evil:Class[_] = _

	"A Footlights class loader, " - {
		"when loading a core class, " - {
			"should load the correct class." in { core.getCanonicalName should equal(CoreClassName) }
			"should grant the AllPermission." in { core should have (allPermission) }
		}

		"when loading a " - {
			"well-behaved app," - {
				"should load the correct app." in {
					good.getCanonicalName should equal (BasicDemo.className)
				}

				"should not grant the AllPermission." in {
					good should not have allPermission
				}

				"should not grant the 'exitVM' permission." in {
					good should not have permission(exitVM)
				}

				"should not grant the right to read core classpaths." in {
					good should not have permissions(readCore)
				}

				"should provide an executable app." in {
					val init = good.getMethods find { method =>
						java.lang.reflect.Modifier.isStatic(method.getModifiers) &&
						method.getName == "init" &&
						method.getParameterTypes.length == 3
					} orElse { fail("Unable to find static init() method in " + good) }

					val log = mock[java.util.logging.Logger]
					val app = init map { _.invoke(null, null, null, log) }

					verify(log, atLeastOnce).info(anyString)
				}

				"should be able to load resources from a JAR file." in {
					val appLoader = good.getClassLoader
					val url = appLoader getResource BasicDemo.classFile
					url should not equal null

					// Check the Java class file magic (0xCAFEBABE).
					val buffer = ByteBuffer allocate 4
					Channels newChannel url.openStream read buffer
					buffer.flip should equal(
							ByteBuffer.wrap(Seq(0xCA, 0xFE, 0xBA, 0xBE) map { _.toByte } toArray))
				}

				"should be able to load a second instance of the same app." in {
					val c = loader loadApplication (BasicDemo.classPath, BasicDemo.className)
					c should not equal good
				}
			}

			"malicious app, " - {
				"should prevent the app from loading unauthorized resources." in
					intercept[java.io.FileNotFoundException] {
						evil.getClassLoader getResource BasicDemo.classFile openStream
					}
			}
		}
	}

	before {
		loader = new FootlightsClassLoader(coreClasspaths map localPath map { new URL(_) } toSeq)
		core = loader loadClass CoreClassName
		good = loader loadApplication (BasicDemo.classPath, BasicDemo.className)
		evil = loader loadApplication (WickedDemo.classPath, WickedDemo.className)
	}


	/** Classpaths: we require the API, Bootstrap and Core classpaths to be set correctly. */
	val coreClasspaths = System getProperty "java.class.path" split ":" filter isCore

	val CoreClassName = "me.footlights.core.Kernel"
	val BasicDemo = App("Basic", "basic-demo", "me.footlights.demos.good.GoodApp")
	val WickedDemo = App("Wicked", "wicked-app", "me.footlights.demos.wicked.WickedApp")


	// Permissions which the ClassLoader can grant.
	val allPermission = permission(new AllPermission)
	val exitVM = new RuntimePermission("exitVM")
	val readCore = coreClasspaths map { new java.io.FilePermission(_, "read") }


	/** Build a {@link HavePropertyMatcher} for "x should not have permission(y)" clauses. */
	def permission(p:Permission) = permissions(Seq(p))

	/** Build a {@link HavePropertyMatcher} for "x should not have permissions(y)" clauses. */
	def permissions(perms:Seq[Permission]) =
		new HavePropertyMatcher[Class[_], Seq[Permission]] {
			def apply(c:Class[_]) =
				HavePropertyMatchResult(
					hasAll(c.getProtectionDomain.getPermissions, perms.iterator),
					"permission",
					perms,
					c.getProtectionDomain.getPermissions.elements.toSeq
				)

			def hasAll(has:PermissionCollection, needs:Iterator[Permission]) =
				needs map { has implies _ } reduce { _ && _ }
		}


	/** Class and path information for a Footlights application. */
	case class App(projectDir:String, projectName:String, className:String) {
		val classFile = className.replaceAll("\\.", "/") + ".class"
		val classPath =
			coreClasspaths find { _ contains "Bootstrap" } map {
				_.replaceFirst("Bootstrap/.*", "Demos/")
			} map {
				_ + projectDir + "/target/" + projectName + "-HEAD.jar"
			} map localPath map { new URL(_) } get
	}



	/** Is the given classpath for core Footlights classes? */
	private def isCore(path:String) =
		List("API", "Bootstrap", "Core") map { path contains _} reduce { _ || _ }

	/** Ensure that a path refers to a local file (uses the 'file' protocol). */
	private def localPath(path:String) = if (path startsWith "/") "file:" + path else path
}

}

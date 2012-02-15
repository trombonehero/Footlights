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
	before { loader = new FootlightsClassLoader(coreUrls) }

	"A Footlights class loader, " - {
		"when loading a core class, " - {
			"should load the correct class." in {
				val loaded = loader loadClass coreClassName
				loaded.getCanonicalName should equal(coreClassName)
			}

			"should grant the AllPermission." in {
				val loaded = loader loadClass coreClassName
				loaded should have (allPermission)
			}
		}

		"when loading a " - {
			"well-behaved app," - {
				"should load the correct app." in {
					val c = loader loadClass BasicDemo.uri
					c.getCanonicalName should equal (BasicDemo.className)
				}

				"should not grant the AllPermission." in {
					val c = loader loadClass BasicDemo.uri
					c should not have allPermission
				}

				"should not grant the 'exitVM' permission." in {
					val c = loader loadClass BasicDemo.uri
					c should not have permission(exitVM)
				}

				"should not grant the right to read core classpaths." in {
					val c = loader loadClass BasicDemo.uri
					c should not have permissions(readCore)
				}

				"should provide an executable app." in {
					val c = loader loadClass BasicDemo.uri

					val init = c.getMethods find { method =>
						java.lang.reflect.Modifier.isStatic(method.getModifiers) &&
						method.getName == "init" &&
						method.getParameterTypes.length == 3
					} orElse { fail("Unable to find static init() method in " + c) }

					val log = mock[java.util.logging.Logger]
					val app = init map { _.invoke(null, null, null, log) }

					verify(log, atLeastOnce).info(anyString)
				}

				"should be able to load resources from a JAR file." in {
					val c = loader loadClass BasicDemo.uri
					val appLoader = c.getClassLoader
					val url = appLoader getResource BasicDemo.classFile
					url should not equal null

					// Check the Java class file magic (0xCAFEBABE).
					val buffer = ByteBuffer allocate 4
					Channels newChannel url.openStream read buffer
					buffer.flip should equal(
							ByteBuffer.wrap(Seq(0xCA, 0xFE, 0xBA, 0xBE) map { _.toByte } toArray))
				}

				"should be able to load a second instance of the same app." in {
					val c1 = loader loadClass BasicDemo.uri
					val c2 = loader loadClass BasicDemo.uri

					c1 should not equal c2
				}
			}

			"malicious app, " - {
				"should prevent the app from loading other apps' classes." in {
					val good = loader loadClass BasicDemo.uri
					val wicked = loader loadClass WickedDemo.uri
					val wickedLoader = wicked.getClassLoader

					intercept[ClassNotFoundException] {
						wickedLoader loadClass BasicDemo.className
					}
				}

				"should prevent the app from loading unauthorized resources." in {
					val good = loader loadClass BasicDemo.uri
					val wicked = loader loadClass WickedDemo.uri
					val wickedLoader = wicked.getClassLoader

					intercept[java.io.FileNotFoundException] {
						wickedLoader getResource BasicDemo.classFile openStream
					}
				}
			}
		}
	}


	def permission(p:Permission) = permissions(Seq(p))

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


	private var loader:FootlightsClassLoader = _

	val coreClasspaths = System getProperty "java.class.path" split ":" filter isCore
	val coreUrls = coreClasspaths map localPath map { new URL(_) } toList

	val allPermission = permission(new AllPermission)
	val exitVM = new RuntimePermission("exitVM")
	val readCore = coreClasspaths map { new java.io.FilePermission(_, "read") }

	/** A well-behaved demo app. */
	val BasicDemo = App("Basic", "basic-demo", "me.footlights.demos.good.GoodApp")

	/** A malicious demo app. */
	val WickedDemo = App("Wicked", "wicked-app", "me.footlights.demos.wicked.WickedApp")

	/** Class and path information for a Footlights application. */
	case class App(projectDir:String, projectName:String, className:String) {
		val classFile = className.replaceAll("\\.", "/") + ".class"
		val uri =
			coreClasspaths find { _ contains "Bootstrap" } map {
				_.replaceFirst("Bootstrap/.*", "Demos/")
			} map {
				_ + projectDir + "/target/" + projectName + "-HEAD.jar!/" + className
			} map localPath get
	}


	/** An example of a core class name. */
	val coreClassName = "me.footlights.core.Kernel"


	/** Is the given classpath for core Footlights classes? */
	private def isCore(path:String) =
		List("API", "Bootstrap", "Core") map { path contains _} reduce { _ || _ }

	/** Ensure that a path refers to a local file (uses the 'file' protocol). */
	private def localPath(path:String) = if (path startsWith "/") "file:" + path else path
}

}

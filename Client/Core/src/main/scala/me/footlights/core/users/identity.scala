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
import java.util.logging

import scala.collection.mutable.{Map => MutableMap}

import me.footlights.api
import me.footlights.api.support.Either._

import me.footlights.core
import me.footlights.core.crypto


package me.footlights.core.users {

/**
 * A user identity: contains a key (public or private), some attributes and a root directory
 * where things are stored (like content we are sharing with the identity).
 */
class UserIdentity(key:crypto.Identity, val attributes:api.ModifiablePreferences,
		val root:api.Directory) {
	def name() = attributes getString "name" getOrElse "<unknown name>"
	val fingerprint = key.fingerprint
	val canSign = key.canSign

	def sign(fingerprint:crypto.Fingerprint) =
		key match {
			case signer:crypto.SigningIdentity => Right(signer sign fingerprint)
			case _ => Left(new UnsupportedOperationException("%s cannot sign content" format this))
		}

	override def toString =
		"'%s' (%s)" format (name(), key.fingerprint.encode.split(":").last.slice(0, 6))
}

object UserIdentity {
	def apply(x:(String, api.Directory)) = x match { case (name, dir) =>
		val key = dir open KeyFile map { _.copyContents } flatMap crypto.Identity.parse

		val saveAttributes = (b:java.nio.ByteBuffer) => dir.save(AttributesFile, b)
		val attrs =
			dir open AttributesFile map { _.copyContents } map
				core.Preferences.parse leftFlatMap {
					// Ignore FileNotFoundException: we just haven't created anything yet!
					ex => ex match {
						case f:java.io.FileNotFoundException => Right(MutableMap[String,String]())
						case e:Exception =>
							log.log(logging.Level.WARNING, "Error loading user attributes", e)
							Left(e)
					}
				} map { currentValues =>
				core.ModifiableStorageEngine.apply(currentValues, Some(saveAttributes))
			}

		key flatMap { key =>
			attrs map { attributes =>
				new UserIdentity(key, attributes, dir)
			}
		}
	}

	def generate(ids:api.Directory, attributes:(String,String)*): Either[Exception,UserIdentity] =
		generate(ids, attributes.toMap)

	def generate(ids:api.Directory, attributes:Map[String,String]) = {
		val key = crypto.Identity.generate()

		ids mkdir key.fingerprint.encode map { d =>
			d.save(KeyFile, key.getBytes)

			val saveAttrs = (bytes:java.nio.ByteBuffer) => d.save(AttributesFile, bytes)
			val attrs = core.ModifiableStorageEngine(MutableMap(), Some(saveAttrs))

			attributes foreach { case (k,v) => attrs.set(k,v) }

			new UserIdentity(key, attrs, d)
		}
	}

	private val AttributesFile = "attributes"
	private val KeyFile = "key"

	private val log = logging.Logger getLogger classOf[UserIdentity].getCanonicalName
}

}

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

import scala.collection.JavaConversions._

import me.footlights.api
import me.footlights.api.support.Either._

import me.footlights.core
import me.footlights.core.crypto
import me.footlights.core.data


package me.footlights.core.users {

/** Manages user identities. */
trait IdentityManagement extends core.Footlights {
	protected def keychain:crypto.MutableKeychain


	override def identities =
		root.subdirs map { case (name, dir) =>
			dir open PrivateKeyFilename orElse { dir open PublicKeyFilename } map {
				_.copyContents } flatMap
				crypto.Identity.parse fold(
					ex => {
						log.log(logging.Level.WARNING, "Error in identity '%s'" format name, ex)
						None
					},
					id => Some(id)
				)
		} flatten

	/** The root directory where application data is stored. */
	private lazy val root = subsystemRoot("identities")

	private val PublicKeyFilename = "public-key"
	private val PrivateKeyFilename = "private-key"

	private val log = logging.Logger getLogger classOf[IdentityManagement].getCanonicalName
}

}

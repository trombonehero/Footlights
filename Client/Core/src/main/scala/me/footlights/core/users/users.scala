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
import me.footlights.core.data


package me.footlights.core.users {

/** Manages user identities. */
trait IdentityManagement extends core.Footlights {
	override def identities = {
		val official =
		root.subdirs map UserIdentity.apply map {
			_ fold (
				ex => {
					log.log(logging.Level.WARNING, "Error in saved identity", ex)
					None
				},
				id => Some(id)
			)
		} flatten

		if (official.nonEmpty) official else {
			// If we don't have any real users, create some fake ones for the sake of the demo.
			UserIdentity.generate(root, "name" -> "Alice") ::
			UserIdentity.generate(root, "name" -> "Bob") ::
			Nil map { _.get }
		}
	}

	override def identity(uri:java.net.URI) =
		root openDirectory uri.toString map { (uri.toString, _) } flatMap UserIdentity.apply

	// TODO: something more sophisticated (choose identity to sign with?)
	override def identity =
		identities find { _.canSign } map Right.apply getOrElse { UserIdentity generate root }


	override def share(d:api.Directory): Either[Exception,java.net.URI] = {
		val ids = identities
		val userMap = identities map { id => (id.toString -> id) } toMap

		promptUser("Who would you like to share with?", "Choose user", userMap, None) map
			share(d match { case mutable:data.MutableDirectory => mutable })
	}

	private def share(dir:data.MutableDirectory)(user:UserIdentity) = {
		log info { "Sharing %s with %s" format (dir, user) }
		val link = dir.dir.link

		user.root subdir OutboundShareDir map { case root:data.MutableDirectory =>
			root.save(link.fingerprint.encode, dir)

			identity flatMap {
				_ sign root.dir.link.fingerprint } map { signature =>
				Map(
					"fingerprint" -> link.fingerprint.encode,
					"key" -> link.key.toUri.toString,
					"signature" -> signature.uri.toString
				)
			} fold (
				ex => log log (logging.Level.WARNING, "Error sharing with %s" format user.name, ex),
				rootInfo =>
					log info { "Updated root for %s: %s" format (user.name, rootInfo) }
					// TODO: actually signal the recipient somehow
			)
		}

		user.fingerprint.toURI
	}

	/** The root directory where application data is stored. */
	private lazy val root = subsystemRoot("identities")

	private val log = logging.Logger getLogger classOf[IdentityManagement].getCanonicalName

	/** A directory containing everything that this user has shared with me. */
	private val InboundShareDir = "shared-with-me"

	/** A directory containing everything that I am sharing with this user. */
	private val OutboundShareDir = "shared"
}

}

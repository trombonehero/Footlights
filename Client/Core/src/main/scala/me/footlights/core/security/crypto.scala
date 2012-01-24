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

import java.security.{Provider,Security}

import org.bouncycastle.jce.provider.BouncyCastleProvider


package me.footlights.core.security {

object CryptoBackend {
	def get:Option[Provider] = synchronized {
		// Convert Java-style "null" provider to a scala None.
		Some(Security.getProvider("BC")) flatMap {
			_ match {
				case p:Provider => Some(p)
				case null => None;
			}
		} orElse {
			// If we haven't already installed BouncyCastle as the crypto provider, we must be
			// running in a special environment such as a unit test.
			val p = Some(new BouncyCastleProvider())
			p foreach { Security.addProvider(_) }
			p
		}
	}
}

}

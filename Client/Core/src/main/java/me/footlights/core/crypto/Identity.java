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
package me.footlights.core.crypto;

import java.security.PublicKey;
import java.security.cert.Certificate;


/** A self-declared identity, represented by a self-signed certificate. */
public class Identity
{
	public Certificate getCertificate() { return certificate; }
	public PublicKey publicKey() { return certificate.getPublicKey(); }

	protected Identity(Certificate certificate)
	{
		this.certificate = certificate;
	}

	@Override public boolean equals(Object other)
	{
		if (!(other instanceof Identity)) return false;
		Identity o = (Identity) other;

		return certificate.equals(o.certificate);
	}

	private final Certificate certificate;
}

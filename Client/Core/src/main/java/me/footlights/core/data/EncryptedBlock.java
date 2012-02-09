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
package me.footlights.core.data;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import me.footlights.core.Preconditions;
import me.footlights.core.crypto.Fingerprint;


/**
 * Encrypted block data.
 * @author Jonathan Anderson (jon@footlights.me)
 */
public class EncryptedBlock
{
	public static class Builder
	{
		public EncryptedBlock build() throws GeneralSecurityException
		{
			return new EncryptedBlock(ciphertext, link.decrypt(ciphertext), link);
		}

		public Builder setCiphertext(ByteBuffer ciphertext)
		{
			this.ciphertext = ciphertext;
			return this;
		}
		public Builder setLink(Link link)
		{
			this.link = link;
			return this;
		}

		private Builder() {}

		private ByteBuffer ciphertext;
		private Link link;
	}

	public static Builder newBuilder() { return new Builder(); }

	/** The name of the block. */
	public Fingerprint name() { return link.fingerprint(); }
	@Override public String toString() { return name().toString(); }

	/** The ciphertext itself. */
	public ByteBuffer ciphertext() { return ciphertext.asReadOnlyBuffer(); }

	/** Decrypted plaintext. */
	public Block plaintext() { return plaintext; }

	/** A link to the ciphertext. */
	public Link link() { return link; }


	@Override public boolean equals(Object o)
	{
		if (o == null) return false;
		if (!(o instanceof EncryptedBlock)) return false;
		EncryptedBlock e = (EncryptedBlock) o;

		if (!link.equals(e.link)) return false;
		if (!ciphertext.equals(e.ciphertext)) return false;
		if (!plaintext.equals(e.plaintext)) return false;

		return true;
	}

	private EncryptedBlock(ByteBuffer ciphertext, Block plaintext, Link link)
	{
		Preconditions.notNull(ciphertext, plaintext, link);

		this.ciphertext = ciphertext.asReadOnlyBuffer();
		this.plaintext = plaintext;
		this.link = link;
	}

	private final ByteBuffer ciphertext;
	private final Block plaintext;
	private final Link link;
}

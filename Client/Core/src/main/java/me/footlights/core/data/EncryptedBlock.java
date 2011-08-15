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


/**
 * Encrypted block data.
 * @author Jonathan Anderson (jon@footlights.me)
 */
public class EncryptedBlock
{
	public static class Builder
	{
		public EncryptedBlock build()
		{
			return new EncryptedBlock(ciphertext, link);
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
	public String name() { return link.uri().toString(); }

	/** The ciphertext itself. */
	public ByteBuffer ciphertext() { return ciphertext.asReadOnlyBuffer(); }

	/** A link to the ciphertext. */
	public Link link() { return link; }


	private EncryptedBlock(ByteBuffer ciphertext, Link link)
	{
		if (link == null) throw new NullPointerException("Link to EncryptedBlock must not be null");

		this.ciphertext = ciphertext.asReadOnlyBuffer();
		this.link = link;
	}

	private final ByteBuffer ciphertext;
	private final Link link;
}

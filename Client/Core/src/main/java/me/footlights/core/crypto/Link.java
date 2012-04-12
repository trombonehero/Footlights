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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;

import me.footlights.core.Preconditions;
import me.footlights.core.Util;
import me.footlights.core.data.Block;
import me.footlights.core.data.FootlightsPrimitive;
import me.footlights.core.data.FormatException;


/**
 * A link between two blocks of data.
 * @author Jonathan Anderson (jon@footlights.me)
 */
public class Link implements FootlightsPrimitive
{
	/** Builds a {@link Link}. */
	public static class Builder
	{
		public Builder setFingerprint(Fingerprint fingerprint)
		{
			this.fingerprint = fingerprint;
			return this;
		}

		public Builder setKey(SecretKey key)
		{
			this.key = key;
			return this;
		}

		/**
		 * Parse a {@link Link} from bytes on the wire.
		 *
		 * This method will advance the {@link ByteBuffer}'s position; callers
		 * who wish to defend against this side effect may wish to use
		 * {@link ByteBuffer#slice()}, {@link ByteBuffer#duplicate()} or
		 * {@link ByteBuffer#asReadOnlyBuffer()}.
		 */
		public Builder parse(ByteBuffer b) throws FormatException, GeneralSecurityException
		{
			Util.setByteOrder(b);

			byte[] magic = new byte[MAGIC.length];
			try
			{
				b.get(magic);
				if (!Arrays.equals(MAGIC, magic))
				{
					throw new FormatException("Invalid Link (bad magic)");
				}

				short bodyLength = b.getShort();

				short fingerprintLength = b.getShort();
				short algorithmLength = b.getShort();
				short keyBits = b.getShort();
				int keyBytes = (keyBits / 8) + ((keyBits % 8 != 0) ? 1 : 0);

				if (fingerprintLength <= 0) throw new FormatException("Cannot link to nothing");

				int subtotal = fingerprintLength + algorithmLength + keyBytes;
				if (bodyLength != subtotal)
				{
					throw new FormatException(
						"Link length (" + bodyLength + ") != sum of its parts ("
						+ subtotal + ")");
				}

				byte[] bytes = new byte[fingerprintLength];
				b.get(bytes);
				fingerprint = Fingerprint.decode(new String(bytes));

				String algorithm = "";
				if (algorithmLength > 0)
				{
					bytes = new byte[algorithmLength];
					b.get(bytes);
					algorithm = new String(bytes);
				}

				byte[] encodedKey = new byte[keyBytes];
				b.get(encodedKey);

				key = SecretKey.newGenerator()
					.setAlgorithm(algorithm)
					.setBytes(encodedKey)
					.generate();
			}
			catch (java.nio.BufferUnderflowException e)
			{
				throw new FormatException(
					"ByteBuffer passed to Link.Builder.parse() is too small", e);
			}

			return this;
		}

		public Link build() { return new Link(fingerprint, key); }

		private Fingerprint fingerprint;
		private SecretKey key;
	}

	public static Builder newBuilder() { return new Builder(); }

	/** Shorthand for {@link Builder#parse().build()}. */
	public static Link parse(ByteBuffer buffer) throws FormatException, GeneralSecurityException
	{
		return newBuilder().parse(buffer).build();
	}


	public Fingerprint fingerprint() { return fingerprint; }
	SecretKey key() { return key; }


	public Block decrypt(ByteBuffer ciphertext) throws GeneralSecurityException
	{
		if (ciphertext.remaining() == 0)
			throw new GeneralSecurityException("Nothing to decrypt!");

		if (cipher == null)
			cipher = key.newCipherBuilder()
				.setOperation(SecretKey.Operation.DECRYPT)
				.build();

		int toDecrypt = cipher.getOutputSize(ciphertext.remaining());
		ByteBuffer plaintext = ByteBuffer.allocate(toDecrypt);
		int bytes = cipher.doFinal(ciphertext.asReadOnlyBuffer(), plaintext);
		if (bytes != toDecrypt)
			throw new GeneralSecurityException(
				"Decrypted wrong number of bytes; expected " + toDecrypt + ", got " + bytes);

		plaintext.flip();
		try { return Block.parse(plaintext); }
		catch (FormatException e)
		{
			throw new GeneralSecurityException("Unable to decrypt", e);
		}
	}

	/**
	 * Default constructor
	 * 
	 * @param algorithm  The algorithm used to encrypt the block (or null)
	 * @param uri        URI of the linked block (often relative, a Base64 hash)
	 * @param key        Encoded decryption key (or null)
	 *
	 * @throws IllegalArgumentException if uri is null
	 */
	private Link(Fingerprint fingerprint, SecretKey key)
	{
		Preconditions.notNull(fingerprint, key);

		this.fingerprint = fingerprint;
		this.key = key;
	}


	private ByteBuffer generateRawBytes()
	{
		final String name = fingerprint.encode();
		final String encryptionAlgorithm = key.getAlgorithm();
		final byte[] keyBytes = key.getKey().getEncoded();

		final short bodyLength = (short)
			(name.length() + encryptionAlgorithm.length() + keyBytes.length);

		final short totalLength = (short) (minimumLength() + bodyLength);

		ByteBuffer buffer = ByteBuffer.allocate(totalLength);
		Util.setByteOrder(buffer);
		buffer.put(MAGIC);
		buffer.putShort(bodyLength);
		buffer.putShort((short) name.length());
		buffer.putShort((short) encryptionAlgorithm.length());
		buffer.putShort((short) (8 * keyBytes.length));  // TODO: finer-grained lengths

		try
		{
			buffer.put(name.getBytes(ASCII));
			buffer.put(encryptionAlgorithm.getBytes(ASCII));
			buffer.put(keyBytes);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(
				"getBytes(ASCII) failed - this should Never Happen");
		}

		return buffer.asReadOnlyBuffer();
	}

	
	// FootlightsPrimitive implementation
	public int bytes()
	{
		if (raw == null) raw = generateRawBytes();
		return raw.limit();
	}

	public ByteBuffer getBytes()
	{
		if (raw == null) raw = generateRawBytes();

		ByteBuffer result = raw.asReadOnlyBuffer();
		result.position(0);
		Util.setByteOrder(result);
		return result;
	}

	static int minimumLength() { return MAGIC.length + 8; }


	// Object overrides
	@Override public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("Link { fingerprint: '");
		buf.append(fingerprint);
		buf.append("', key: ");
		buf.append((key == null) ? "<null>" : key.toString());
		buf.append(" }");

		return buf.toString();
	}

	@Override public boolean equals(Object o)
	{
		if (!(o instanceof Link)) return false;
		Link other = (Link) o;

		if (!fingerprint.equals(other.fingerprint)) return false;
		if (!key.getAlgorithm().equals(other.key.getAlgorithm())) return false;

		byte[] myBytes = key.getKey().getEncoded();
		byte[] otherBytes = other.key.getKey().getEncoded();
		if (myBytes.length != otherBytes.length) return false;
		for (int i = 0; i < myBytes.length; i++)
			if (myBytes[i] != otherBytes[i]) return false;

		return true;
	}


	/** The magic bytes at the beginning of a {@link Link}. */
	private static byte[] MAGIC = new byte[] { 'L', 'I', 'N', 'K', '\r', '\n' };

	/** Character set used for byte<->String translation. */
	private static final String ASCII = "ascii";

	/** Name of the block. */
	private final Fingerprint fingerprint;

	/** Key to decrypt the linked block (or null) */
	private final SecretKey key;

	/** Cipher used to decrypt the linked block. */
	private Cipher cipher;

	/** Raw byte representation */
	private ByteBuffer raw;
}

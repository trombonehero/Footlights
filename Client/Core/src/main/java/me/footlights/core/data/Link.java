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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;

import me.footlights.core.Preconditions;
import me.footlights.core.Util;
import me.footlights.core.crypto.SecretKey;
import me.footlights.core.crypto.SecretKey.Operation;


/**
 * A link between two blocks of data.
 * @author Jonathan Anderson (jon@footlights.me)
 */
public class Link implements FootlightsPrimitive
{
	/** Builds a {@link Link}. */
	public static class Builder
	{
		public Builder setAlgorithm(String algorithm)
		{
			this.algorithm = algorithm;
			return this;
		}

		public Builder setUri(URI uri)
		{
			this.uri = uri;
			return this;
		}

		public Builder setKey(byte[] key)
		{
			this.key = key;
			return this;
		}

		public Builder setIv(byte[] iv)
		{
			this.iv = iv;
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
		public Builder parse(ByteBuffer b) throws FormatException
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

				short algorithmLength = b.getShort();
				short uriLength = b.getShort();
				short keyBits = b.getShort();
				int keyBytes = (keyBits / 8) + ((keyBits % 8 != 0) ? 1 : 0);
				short ivBits = b.getShort();
				int ivBytes = (ivBits / 8) + ((ivBits % 8 != 0) ? 1 : 0);

				int subtotal = algorithmLength + uriLength + keyBytes + ivBytes;
				if (bodyLength != subtotal)
				{
					throw new FormatException(
						"Link length (" + bodyLength + ") != sum of its parts ("
						+ subtotal + ")");
				}
	
				if (algorithmLength > 0)
				{
					byte[] bytes = new byte[algorithmLength];
					b.get(bytes);
					algorithm = new String(bytes);
				}
	
				if (uriLength > 0)
				{
					byte[] bytes = new byte[uriLength];
					b.get(bytes);
					uri = URI.create(new String(bytes));
				}
	
				key = new byte[keyBytes];
				b.get(key);
	
				iv = new byte[ivBytes];
				b.get(iv);
			}
			catch (java.nio.BufferUnderflowException e)
			{
				throw new FormatException(
					"ByteBuffer passed to Link.Builder.parse() is too small", e);
			}

			return this;
		}

		public Link build()
		{
			return new Link(
				algorithm != null ? algorithm : "",
				uri,
				key != null ? key : new byte[0],
				iv != null ? iv : new byte[0]);
		}

		private String algorithm;
		private URI uri;
		private byte[] key;
		private byte[] iv;
	}

	public static Builder newBuilder() { return new Builder(); }

	/** Shorthand for {@link Builder#parse().build()}. */
	public static Link parse(ByteBuffer buffer) throws FormatException
	{
		return newBuilder().parse(buffer).build();
	}


	public String algorithm() { return algorithms; }
	public URI uri() { return uri; }
	public byte[] key() { return Arrays.copyOf(key, key.length); }
	public byte[] iv() { return Arrays.copyOf(iv, iv.length); }


	public Block decrypt(ByteBuffer ciphertext) throws GeneralSecurityException
	{
		if (cipher == null)
			cipher = SecretKey.newGenerator()
				.setAlgorithm(algorithms)
				.setBytes(key)
				.generate()
				.newCipherBuilder()
				.setOperation(Operation.DECRYPT)
				.setInitializationVector(iv)
				.build();

		int toDecrypt = cipher.getOutputSize(ciphertext.remaining());
		ByteBuffer plaintext = ByteBuffer.allocate(toDecrypt);
		cipher.doFinal(ciphertext, plaintext);

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
	 * @param iv         Initialization vector (null & key => all-zero IV)
	 *
	 * @throws IllegalArgumentException if uri is null
	 */
	private Link(String algorithm, URI uri, byte[] key, byte[] iv)
	{
		Preconditions.notNull(algorithm, uri, key, iv);

		if(uri.toString().length() == 0)
			throw new IllegalArgumentException("Link has no URI");

		this.algorithms = algorithm;
		this.uri = uri;
		this.key = key;
		this.iv = iv;
	}


	private ByteBuffer generateRawBytes()
	{
		final short bodyLength = (short)
			(algorithms.length()
			 + uri.toString().length()
			 + key.length
		    + iv.length);

		final short totalLength = (short) (MAGIC.length + 10 + bodyLength);

		ByteBuffer buffer = ByteBuffer.allocate(totalLength);
		Util.setByteOrder(buffer);
		buffer.put(MAGIC);
		buffer.putShort(bodyLength);
		buffer.putShort((short) algorithms.length());
		buffer.putShort((short) uri.toString().length());
		buffer.putShort((short) (8 * key.length));  // TODO: finer-grained lengths
		buffer.putShort((short) iv.length);

		try
		{
			buffer.put(algorithms.getBytes(ASCII));
			buffer.put(uri.toString().getBytes(ASCII));
			buffer.put(key);
			buffer.put(iv);
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
		if(raw == null) raw = generateRawBytes();
		return raw.limit();
	}

	public ByteBuffer getBytes()
	{
		if(raw == null) raw = generateRawBytes();

		ByteBuffer result = raw.asReadOnlyBuffer();
		result.position(0);
		Util.setByteOrder(result);
		return result;
	}

	// Object overrides
	@Override public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("Link { alg: '");
		buf.append(algorithms);
		buf.append("', uri: '");
		buf.append(uri);
		buf.append("', key: ");
		buf.append((key == null) ? "<null>" : "<key>");
		buf.append(", iv: ");
		if (iv == null) buf.append("<null>");
		else
		{
			buf.append("[");
			for (byte b : iv) buf.append((0xff & b) + " ");
			buf.append("] ");
		}
		buf.append(" }");

		return buf.toString();
	}

	@Override public boolean equals(Object o)
	{
		try
		{
			Link other = (Link) o;
			if((other.uri == null) ^ (uri == null)) return false;
			if((uri != null) && !other.uri.equals(other.uri)) return false;
			
			if((other.algorithms == null) ^ (algorithms == null)) return false;
			if((algorithms != null) && !algorithms.equals(other.algorithms))
				return false;

			if((other.key == null) ^ (key == null)) return false;
			if((key != null) && (other.key.length != key.length)) return false;

			int len = (key == null) ? 0 : key.length;
			for(int i = 0; i < len; i++)
				if(other.key[i] != key[i]) return false;
			
			return true;
		}
		catch(ClassCastException e) { return false; }
	}


	/** The magic bytes at the beginning of a {@link Link}. */
	private static byte[] MAGIC = new byte[] { 'L', 'I', 'N', 'K', '\r', '\n' };

	/** Character set used for byte<->String translation. */
	private static final String ASCII = "ascii";


	/** URI of the linked block (often just a Base64-encoded hash) */
	private final URI uri;

	/** Algorithm used to decrypt the linked block (or null) */
	private final String algorithms;

	/** Key to decrypt the linked block (or null) */
	private final byte[] key;

	/**
	 * Initialisation vector (null if the block is not encrypted, or has an
	 * all-zero IV).
	 */
	private final byte[] iv;

	/** Cipher used to decrypt the linked block. */
	private Cipher cipher;

	/** Raw byte representation */
	private ByteBuffer raw;
}

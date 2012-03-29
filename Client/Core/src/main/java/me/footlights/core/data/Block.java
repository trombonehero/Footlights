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
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;

import me.footlights.core.Preconditions;
import me.footlights.core.crypto.Fingerprint;
import me.footlights.core.crypto.Link;
import me.footlights.core.crypto.SecretKey;


/**
 * A block of user data and, optionally, links to other blocks.
 * @author Jonathan Anderson (jon@footlights.me)
 */
public class Block implements FootlightsPrimitive
{
	public static class Builder
	{
		/** How much room is left in this {@link Block}-to-be? */
		public int remaining()
		{
			int remaining = desiredSize - MAGIC.length - 10
					- (padding == null ? 0 : padding.limit());

			for (Link link : links) remaining -= link.bytes();
			for (ByteBuffer b : content) remaining -= b.remaining();

			return remaining;
		}

		public Builder addLink(Link link) { links.add(link); return this; }

		public Builder addContent(byte[] content) { return addContent(ByteBuffer.wrap(content)); }
		public Builder addContent(ByteBuffer content)
		{
			this.content.add(content.asReadOnlyBuffer());
			return this;
		}

		/**
		 * Specify a particular size for the {@link Block} to end up with.
		 * If unspecified, the size of the {@link Block} will be a power of 2.
		 */
		public Builder setDesiredSize(int bytes)
		{
			this.desiredSize = bytes;
			return this;
		}

		public Builder setNamingAlgorithm(String a) throws NoSuchAlgorithmException
		{
			fingerprintBuilder.setAlgorithm(a);
			return this;
		}

		public Builder parse(ByteBuffer bytes) throws FormatException, NoSuchAlgorithmException
		{
			int startPosition = bytes.position();

			byte[] magic = new byte[MAGIC.length];
			bytes.get(magic);
			if (!Arrays.equals(MAGIC, magic))
				throw new FormatException(
					"Block does not begin with the correct magic");

			byte n = bytes.get();
			if (n < 0) throw new FormatException("Negative N: " + n);
			long length = (1 << n);
			if (length < MINIMUM_BYTES)
				throw new FormatException(
					"Block cannot be smaller than " + MINIMUM_BYTES + "B");

			byte linkCount = bytes.get();
			if (linkCount < 0)
				throw new FormatException("Negative linkCount: " + linkCount);

			int dataOffset = bytes.getInt();
			if (dataOffset < 0)
				throw new FormatException("Negative offset: " + dataOffset);

			int dataLength = bytes.getInt();
			if (dataLength < 0)
				throw new FormatException("Negative length: " + dataLength);
			
			if (dataOffset + dataLength > length)
				throw new FormatException(
					"Block is not long enough to contain "
					 + dataOffset + " + " + dataLength
					 + "B of user data (total length: " + length + ")");

			links.clear();
			for (byte i = 0; i < linkCount; i++)
				addLink(Link.parse(bytes));

			if (bytes.position() - startPosition != dataOffset)
				throw new FormatException(
					"Incorrect offset (error parsing Links?)");

			ByteBuffer content = bytes.slice().asReadOnlyBuffer();
			content.limit(dataLength);
			this.content.add(content);

			bytes.position(bytes.position() + dataLength);
			padding = bytes.slice();

			return this;
		}

		public Block build() throws FormatException
		{
			final ByteBuffer flattened;
			if (content.size() == 1) flattened = content.get(0);
			else
			{
				int contentSize = 0;
				for (ByteBuffer b : content) contentSize += b.remaining();

				flattened = ByteBuffer.allocate(contentSize);
				for (ByteBuffer b : content) flattened.put(b);
				flattened.flip();
			}

			return new Block(links, flattened, padding, desiredSize, fingerprintBuilder);
		}

		private Builder() {}

		private int desiredSize = 0;
		private List<Link> links = new ArrayList<Link>();
		private List<ByteBuffer> content = new ArrayList<ByteBuffer>();
		private ByteBuffer padding;
		private Fingerprint.Builder fingerprintBuilder = Fingerprint.newBuilder();
	}

	public static Builder newBuilder() { return new Builder(); }

	/** Shorthand for {@link Builder#parse(ByteBuffer).build()}. */
	public static Block parse(ByteBuffer bytes) throws FormatException, NoSuchAlgorithmException
	{
		return newBuilder().parse(bytes).build();
	}

	public List<Link> links() { return links; }
	public ByteBuffer content() { return content.asReadOnlyBuffer(); }
	public Fingerprint name() { return fingerprint; }

	public EncryptedBlock encrypt() throws GeneralSecurityException
	{
		SecretKey.Generator keygen = SecretKey.newGenerator();

		short length = (short) keygen.getKeyLength();
		byte[] hashBytes = fingerprint.copyBytes();
		byte[] secret = new byte[length / 8];
		System.arraycopy(hashBytes, 0, secret, 0, secret.length);

		SecretKey key = keygen
				.setBytes(secret)
				.generate();

		Cipher cipher = key
			.newCipherBuilder()
			.setOperation(SecretKey.Operation.ENCRYPT)
			.build();

		int len = cipher.getOutputSize(bytes.remaining());
		ByteBuffer ciphertext = ByteBuffer.allocate(len);

		cipher.doFinal(bytes.asReadOnlyBuffer(), ciphertext);
		ciphertext.flip();

		Fingerprint ciphertextName = Fingerprint.newBuilder().setContent(ciphertext).build();
		Link link = Link.newBuilder()
			.setFingerprint(ciphertextName)
			.setKey(key)
			.build();

		return EncryptedBlock.newBuilder()
			.setCiphertext(ciphertext)
			.setLink(link)
			.build();
	}


	// FootlightsPrimitive implementation
	public int bytes()            { return bytes.remaining(); }
	public ByteBuffer getBytes()  { return bytes.asReadOnlyBuffer(); }

	// Object overrides
	@Override public boolean equals(Object o)
	{
		if(o == null) return false;
		Block other;

		try { other = (Block) o; }
		catch(ClassCastException e) { return false; }

		if (!fingerprint.equals(other.fingerprint)) return false;
		if (!links.equals(other.links)) return false;

		if ((content == null) ^ (other.content == null)) return false;
		if ((content != null) && !content.equals(other.content))
			return false;

		return true;
	}

	@Override public String toString()
	{
		StringBuffer buf = new StringBuffer();

		buf.append("Block { ");
		buf.append(links.size());
		buf.append(" links, ");
		buf.append((content == null) ? 0 : content.limit());
		buf.append(" B of content }");

		return buf.toString();
	}


	/**
	 * Convert buffers of data, which may have any size, into buffers of a desired chunk size.
	 *
	 * @param  after    leave this many bytes free at the beginning; the first block will be of
	 *                  size (after - chunkSize)
	 */
	static Collection<ByteBuffer> rechunk(Iterable<ByteBuffer> content, int after, int chunkSize)
	{
		Iterator<ByteBuffer> i = content.iterator();
		ByteBuffer next = null;

		List<ByteBuffer> chunked = new LinkedList<ByteBuffer>();
		ByteBuffer current = ByteBuffer.allocate(chunkSize - (after % chunkSize));

		while (true)
		{
			// Fetch the next input buffer (if necessary). If there are none, we're done.
			if ((next == null) || !next.hasRemaining())
			{
				if (i.hasNext()) next = i.next();
				else break;

				// If the next batch of content is already the right size, add it directly.
				if (next.remaining() == chunkSize)
				{
					chunked.add(next);
					next = null;
					continue;
				}
			}

			// If the current output buffer is full, create a new one.
			if (current.remaining() == 0)
			{
				current.flip();
				chunked.add(current);
				current = ByteBuffer.allocate(chunkSize);
			}

			// Copy data from input to output.
			int toCopy = Math.min(next.remaining(), current.remaining());
			next.get(current.array(), current.position(), toCopy);
			current.position(current.position() + toCopy);
		}

		if (current.position() > 0)
		{
			current.flip();
			chunked.add(current);
		}

		return chunked;
	}

	static Collection<ByteBuffer> rechunk(Iterable<ByteBuffer> content, int chunkSize)
	{
		return rechunk(content, 0, chunkSize);
	}


	/**
	 * Private constructor; use {@link #parse} or {@link #newBuilder}.
	 *
	 * @param padding     Random padding at the end of the block. If null,
	 *                    the constructor will generate random padding. If
	 *                    non-null, the padding must be of precisely the
	 *                    correct length to pad the block out to a total length
	 *                    of a power of two: <b>only</b> do this when parsing
	 *                    an existing, correctly-padded block.
	 * @param desiredSize The desired block size, or 0 for "automatically size to a power of 2"
	 */
	private Block(List<Link> links, ByteBuffer content, ByteBuffer padding,
			int desiredSize, Fingerprint.Builder fingerprintBuilder)
		throws FormatException
	{
		Preconditions.notNull(links, content, fingerprintBuilder);

		this.links    = Collections.unmodifiableList(links);

		// How many bytes do we need for the raw byte representation?
		int byteCount = MINIMUM_BYTES;
		for (Link link : links) byteCount += link.bytes();
		int dataOffset = byteCount;
		byteCount += content.remaining();
		if (padding != null) byteCount += padding.remaining();

		// Do we want the block to be a particular size?
		if (desiredSize != 0) byteCount = desiredSize;

		// Calculate N = log_2(length)
		byte N;
		int totalLength = byteCount;
		for (N = 1; N <= 32; N++)
		{
			if (N == 32) throw new FormatException("Block size > 2^32");
			else byteCount = (byteCount >> 1);

			if (byteCount == 1) break;
		}

		// Make the total block size a power of 2
		if (totalLength != (1 << N))
		{
			N++;
			totalLength = (1 << N);
		}

		// Allocate serialized version, write header information
		ByteBuffer rawBytes = ByteBuffer.allocate(totalLength);
		rawBytes.put(MAGIC);
		rawBytes.put(N);
		rawBytes.put((byte) links.size());
		rawBytes.putInt(dataOffset);
		rawBytes.putInt(content.remaining());

		// Write links
		for (Link link : links) rawBytes.put(link.getBytes().asReadOnlyBuffer());

		// The 'content' buffer can share the 'bytes' backing array
		this.content = rawBytes.slice().asReadOnlyBuffer();
		this.content.limit(content.remaining());
		rawBytes.put(content);

		if (padding == null)
		{
			padding = ByteBuffer.allocate(rawBytes.remaining());
			random.nextBytes(padding.array());
		}

		// The padding buffer can also share rawBytes' backing array, but make
		// sure it's the correct length first.
		if (padding.remaining() != rawBytes.remaining())
			throw new IllegalArgumentException(
				"Supplied padding length is incorrect (" + padding.remaining()
				+ "B, should be " + rawBytes.remaining() + "B)");

		this.padding = rawBytes.slice().asReadOnlyBuffer();
		this.padding.limit(padding.remaining());
		rawBytes.put(padding);

		// That's the last of the raw bytes.
		rawBytes.rewind();

		if ((desiredSize != 0) && (rawBytes.remaining() != desiredSize))
			throw new FormatException(
				"Built block of incorrect size: wanted " + desiredSize
				 + "B, was " + rawBytes.remaining() + "B");

		this.bytes = rawBytes;
		if ((desiredSize != 0) && (desiredSize != this.bytes.limit()))
			throw new IllegalArgumentException(
				"Unable to construct a " + desiredSize + "B block; is it a power of 2?"
				 + " The nearest possible block size is " + this.bytes.limit() + "B.");

		// Now that our raw bytes have been completely determined, calculate
		// the block's name (based on a fingerprint of its contents).
		if (fingerprintBuilder == null) fingerprintBuilder = Fingerprint.newBuilder();
		this.fingerprint = fingerprintBuilder.setContent(bytes).build();
	}


	/** Magic bytes which the {@link Block} should start with: F00TDATA\r\n. */
	private static final byte[] MAGIC = new byte[]
	{
		(byte) 0xF0, 0x07, (byte) 0xDA, 0x7A, '\r', '\n',
	};

	/** The smallest possible {@link Block} according to the specification. */
	private static final int MINIMUM_BYTES = 16;

	/** PRNG for padding bytes. */
	private static final Random random = new Random();

	private final Fingerprint fingerprint;
	private final List<Link> links;
	private final ByteBuffer content;
	private final ByteBuffer padding;

	/** Raw byte version of the block */
	private final ByteBuffer bytes;
}

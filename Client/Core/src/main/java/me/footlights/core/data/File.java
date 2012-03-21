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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import me.footlights.core.data.store.Stat;


/**
 * A logical file.
 *
 * Files are immutable; to modify a file, you must create and freeze a {@link MutableFile}.
 */
public class File implements me.footlights.api.File
{
	public static File from(EncryptedBlock header, Collection<EncryptedBlock> ciphertext)
	{
		List<Block> plaintext = new ArrayList<Block>(ciphertext.size());
		for (EncryptedBlock e : ciphertext) plaintext.add(e.plaintext());

		return new File(header, plaintext, ciphertext);
	}

	public static MutableFile newBuilder() { return new MutableFile(); }
	public static final class MutableFile
	{
		public MutableFile setContent(ByteBuffer content)
		{
			this.content = Arrays.asList(content);
			return this;
		}

		public MutableFile setContent(Collection<ByteBuffer> content)
		{
			this.content = content;
			return this;
		}

		MutableFile setBlocks(Collection<Block> content)
		{
			List<ByteBuffer> bytes = new ArrayList<ByteBuffer>(content.size());
			for (Block b : content) bytes.add(b.content());
			this.content = bytes;

			return this;
		}

		MutableFile setDesiredBlockSize(int size)
		{
			this.desiredBlockSize = size;
			return this;
		}

		/**
		 * Produce a proper {@link File} by fixing the current contents of this
		 * {@link MutableFile}.
		 */
		public File freeze() throws FormatException, GeneralSecurityException
		{
			// First, break the content into chunks of the appropriate size.
			Collection<ByteBuffer> chunked =
				rechunk(content, desiredBlockSize - Block.OVERHEAD_BYTES);

			// Next, create {@link EncryptedBlock} objects.
			List<EncryptedBlock> ciphertext = new ArrayList<EncryptedBlock>(chunked.size());

			for (ByteBuffer b : chunked)
				ciphertext.add(
					Block.newBuilder()
						.setContent(b)
						.setDesiredSize(desiredBlockSize)
						.build()
						.encrypt());

			// Finally, create the header. TODO: just embed links in all the blocks.
			Block.Builder header = Block.newBuilder();
			for (EncryptedBlock b : ciphertext) header.addLink(b.link());

			return File.from(header.build().encrypt(), ciphertext);
		}

		private MutableFile() {}

		private Iterable<ByteBuffer> content = new ArrayList<ByteBuffer>();
		private int desiredBlockSize = 4096;
	}

	@Override public URI name() { return stat.name().toURI(); }
	public Stat stat() { return stat; }
	public URI key() { return header.link().key().toUri(); }


	/**
	 * The content of the file, as one big {@link ByteBuffer}.
	 *
	 * Note that, depending on how big the {@link File} is, it might be very silly to actually
	 * call this method.
	 */
	public ByteBuffer getContents() throws IOException
	{
		int len = 0;
		for (Block b : plaintext) len += b.content().remaining();

		ByteBuffer buffer = ByteBuffer.allocateDirect(len);

		for (Block b : plaintext)
			buffer.put(b.content().asReadOnlyBuffer());

		buffer.flip();
		return buffer;
	}


	/**
	 * Get a {@link ReadableByteChannel} for the {@link File}.
	 *
	 * For now, this is a wrapper around {@link #getInputStream()}; in the future, it should be
	 * the other way around (if we keep {@link #getInputStream()} at all).
	 */
	public ReadableByteChannel getChannel() { return Channels.newChannel(getInputStream()); }

	/**
	 * The content of the file, transformed into an {@link InputStream}.
	 */
	@Override public InputStream getInputStream()
	{
		final ByteBuffer[] buffers = new ByteBuffer[plaintext.size()];
		for (int i = 0; i < buffers.length; i++)
			buffers[i] = plaintext.get(i).content();

		return new InputStream()
		{
			@Override public int available()
			{
				int total = 0;
				for (int i = blockIndex; i < buffers.length; i++)
					total += buffers[i].remaining();

				return total;
			}

			@Override public int read(byte[] buffer, int offset, int len)
			{
				if (len == 0) return 0;
				if (blockIndex >= plaintext.size()) return -1;

				int pos = offset;
				while ((pos < (offset + len)) && (blockIndex < buffers.length))
				{
					int leftToRead = len - (pos - offset);
					ByteBuffer next = buffers[blockIndex];

					int bytes = Math.min(leftToRead, next.remaining());
					next.get(buffer, pos, bytes);
					pos += bytes;

					if (next.remaining() == 0) blockIndex++;
					if (pos == offset) return -1;
				}

				return (pos - offset);
			}

			/** This is a horrendously inefficient way of reading data. Don't! */
			@Override public int read() throws IOException
			{
				byte[] data = new byte[1];
				int bytes = read(data, 0, data.length);

				if (bytes < 0) throw new BufferUnderflowException();
				if (bytes == 0)
					throw new Error(
						"Implementation error in File.read(byte[1]): returned 0");

				return data[0];
			}


			private int blockIndex;
		};
	}


	/** Encrypted blocks to be saved in a {@link Store}. */
	public List<EncryptedBlock> toSave()
	{
		LinkedList<EncryptedBlock> everything = new LinkedList<EncryptedBlock>(ciphertext);
		everything.push(header);

		return everything;
	}

	/** A link to the {@link File} itself. */
	public Link link() { return header.link(); }


	EncryptedBlock encryptedHeader() { return header; }

	/**
	 * The contents of the file.
	 *  
	 * @throws IOException    on I/O errors such as network failures
	 */
	List<ByteBuffer> content() throws IOException
	{
		List<ByteBuffer> content = new ArrayList<ByteBuffer>(plaintext.size());
		for (Block b : plaintext) content.add(b.content());

		return content;
	}

	@Override public boolean equals(Object o)
	{
		if (o == null) return false;
		if (!(o instanceof File)) return false;

		File f = (File) o;
		if (!this.header.equals(f.header)) return false;
		if (!this.plaintext.equals(f.plaintext)) return false;
		if (!this.ciphertext.equals(f.ciphertext)) return false;

		return true;
	}

	@Override
	public String toString()
	{
		return "Encrypted File [ " + header.name() + ", " + plaintext.size() + " blocks, " +
			stat.length() + " B ]";
	}


	/** Convert buffers of data, which may have any size, into buffers of a desired chunk size. */
	private static Collection<ByteBuffer> rechunk(Iterable<ByteBuffer> content, int chunkSize)
	{
		Iterator<ByteBuffer> i = content.iterator();
		ByteBuffer next = null;

		List<ByteBuffer> chunked = new LinkedList<ByteBuffer>();
		ByteBuffer current = ByteBuffer.allocate(chunkSize);

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


	/** Default constructor; produces an anonymous file */
	private File(EncryptedBlock header,
		Collection<Block> plaintext, Collection<EncryptedBlock> ciphertext)
	{
		this.header = header;
		this.plaintext = new ArrayList<Block>(plaintext);
		this.ciphertext = new ArrayList<EncryptedBlock>(ciphertext);

		long len = 0;
		for (Block b : plaintext) len += b.content().remaining();
		this.stat = Stat.apply(header.name(), len);
	}


	private final EncryptedBlock header;
	private final List<Block> plaintext;
	private final List<EncryptedBlock> ciphertext;
	private final Stat stat;
}

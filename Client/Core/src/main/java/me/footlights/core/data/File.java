package me.footlights.core.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;


/**
 * A logical file.
 *
 * Files are immutable; to modify a file, you must create and freeze a {@link MutableFile}.
 */
public class File implements me.footlights.File
{
	public static MutableFile newBuilder() { return new MutableFile(); }
	public static class MutableFile
	{
		/** TODO: break content into appropriate-sized blocks. */
		public synchronized MutableFile setContent(Iterable<ByteBuffer> content)
		{
			this.content.clear();
			for (ByteBuffer b : content)
				this.content.add(b.asReadOnlyBuffer());

			return this;
		}

		/**
		 * Produce a proper {@link File} by fixing the current contents of this
		 * {@link MutableFile}.
		 */
		public synchronized File freeze() throws FormatException, GeneralSecurityException
		{
			List<Block> plaintext = Lists.newLinkedList();
			List<EncryptedBlock> ciphertext = Lists.newLinkedList();

			for (ByteBuffer b : content)
			{
				Block block = Block.newBuilder()
					.setContent(b)
					.build();

				plaintext.add(block);
				ciphertext.add(block.encrypt());
			}

			Block.Builder header = Block.newBuilder();
			for (EncryptedBlock b : ciphertext) header.addLink(b.link());

			return new File(header.build().encrypt(), plaintext, ciphertext);
		}

		private MutableFile() { this.content = new Vector<ByteBuffer>(); }

		private final Vector<ByteBuffer> content;
	}


	/**
	 * The content of the file, transformed into an {@link InputStream}.
	 */
	public InputStream getInputStream()
	{
		final ByteBuffer[] buffers = new ByteBuffer[plaintext.size()];
		for (int i = 0; i < buffers.length; i++)
			buffers[i] = plaintext.get(i).content();

		return new InputStream()
		{
			@Override public int read(byte[] buffer, int offset, int len)
			{
				if (len == 0) return 0;
				if (blockIndex >= plaintext.size()) return -1;

				int pos = offset;
				while (pos < len)
				{
					ByteBuffer next = buffers[blockIndex];
					if (next.remaining() > 0)
					{
						int bytes = next.remaining();
						next.get(buffer, pos, bytes);
						pos += bytes;
					}

					if (pos == offset) return -1;
					else blockIndex++;
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
		LinkedList<EncryptedBlock> everything = Lists.newLinkedList(ciphertext);
		everything.push(header);

		return everything;
	}

	/**
	 * The contents of the file.
	 *  
	 * @throws IOException    on I/O errors such as network failures
	 */
	List<ByteBuffer> content() throws IOException
	{
		List<ByteBuffer> content = Lists.newLinkedList();
		for(Block b : plaintext) content.add(b.content());

		return content;
	}

	/** Default constructor; produces an anonymous file */
	private File(EncryptedBlock header,
		List<Block> plaintext, List<EncryptedBlock> ciphertext)
	{
		this.header = header;
		this.plaintext = ImmutableList.<Block>builder().addAll(plaintext).build();
		this.ciphertext = ImmutableList.<EncryptedBlock>builder().addAll(ciphertext).build();
	}


	private final EncryptedBlock header;
	private final ImmutableList<Block> plaintext;
	private final ImmutableList<EncryptedBlock> ciphertext;
}

package me.footlights.core.data.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import me.footlights.core.Core;
import me.footlights.core.data.Block;
import me.footlights.core.data.FormatException;
import me.footlights.core.data.Link;
import me.footlights.core.data.NoSuchBlockException;

public class Fetcher
{
	public static class FetchResult
	{
		static class Builder
		{
			private Builder()
			{
				plaintext = Maps.newLinkedHashMap();
				ciphertext = Maps.newLinkedHashMap();
			}

			public FetchResult build()
			{
				return new FetchResult(plaintext, ciphertext);
			}

			public Builder add(FetchResult other)
			{
				plaintext.putAll(other.plaintextBlocks);
				ciphertext.putAll(other.encryptedBlocks);
				return this;
			}

			public Builder add(String name, Block plaintextBlock)
			{
				if (!plaintextBlock.name().equals(name))
					throw new IllegalArgumentException(
						"Retrieved block name ('" + plaintextBlock.name()
						+ "') != linked-to name ('" + name + "')");

				plaintext.put(name, plaintextBlock);
				return this;
			}

			public Builder add(String name, ByteBuffer ciphertextBlock)
			{
				ciphertext.put(name, ciphertextBlock);
				return this;
			}

			private Map<String, Block> plaintext;
			private Map<String, ByteBuffer> ciphertext;
		}

		static Builder newBuilder() { return new Builder(); }

		private FetchResult(Map<String,Block> plaintext,
			Map<String,ByteBuffer> ciphertext)
		{
			this.plaintextBlocks = Collections.unmodifiableMap(plaintext);
			this.encryptedBlocks = Collections.unmodifiableMap(ciphertext);
		}

		public final Map<String, Block> plaintextBlocks;
		public final Map<String, ByteBuffer> encryptedBlocks;
	}


	public Fetcher(Store store)
	{
		this.store = store;
	}


	/**
	 * Fetch a block, decrypting and recursing as necessary.
	 * 
	 * Any blocks which cannot be decrypted will be left as encrypted blobs.
	 */
	public FetchResult fetch(Link link)
		throws IOException, NoSuchBlockException
	{
		final String name = link.uri().toASCIIString();

		ByteBuffer bytes = store.retrieve(name);
		Block block = null;

		// Is the block already in plaintext?
		try { block = Block.parse(bytes.asReadOnlyBuffer()); }
		catch (FormatException e)
		{
			// Block is encrypted... can we decrypt it?
			try { block = link.decrypt(bytes); }
			catch (GeneralSecurityException gse)
			{
				log.log(Level.WARNING, "Unable to decrypt block", gse);
			}
		}

		FetchResult.Builder result = FetchResult.newBuilder();
		if (block == null) result.add(name, bytes);
		else
		{
			result.add(name, block);

			// retrieve the blocks that this block links to
			for (Link l : block.links())
				result.add(fetch(l));
		}

		return result.build();
	}


	private final Logger log = Logger.getLogger(Core.CORE_LOG_NAME);
	private final Store store;
}

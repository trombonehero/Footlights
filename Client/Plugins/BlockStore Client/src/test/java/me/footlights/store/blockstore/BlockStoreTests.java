package me.footlights.store.blockstore;

import java.net.ConnectException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import me.footlights.core.data.Block;
import me.footlights.core.data.store.MemoryStore;
import me.footlights.core.data.store.Store;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class BlockStoreTests
{
	/** For the moment, the server just checks for a shared secret */
	private static final String SHARED_SECRET = "tuxes26?stye";


	@Test public void testLocalStorage() throws Throwable
	{
		Store cache = new MemoryStore();
		Store store = new BlockStoreClient(
				"d2b0r6sfoq6kur.cloudfront.net",
				new URL("http://localhost:8080/UploadManager/upload"),
				SHARED_SECRET, cache);

		Block b = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 }))
			.build();

		store.store(b);

		try { store.flush(); }
		catch (ConnectException e)
		{
			Logger.getAnonymousLogger().warning(
				"Failed to connect to local upload server; is Tomcat running?");
		}

		assertEquals(b.getBytes(), store.retrieve(b.name()));
	}

	@Test public void testRemoteStorage() throws Throwable
	{
		Store cache = new MemoryStore();
		Store store = new BlockStoreClient(
				"d2b0r6sfoq6kur.cloudfront.net",
				new URL("https://upload.footlights.me/upload"),
				SHARED_SECRET, cache);

		Block b = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 }))
			.build();

		store.store(b);
		store.flush();

		assertEquals(b.getBytes(), store.retrieve(b.name()));
	}
}

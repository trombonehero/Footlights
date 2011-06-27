package me.footlights.store.blockstore;

import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;

import me.footlights.core.Preferences;
import me.footlights.core.data.Block;
import me.footlights.core.data.store.Store;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class BlockStoreTest
{
	@BeforeClass public static void setupClass() throws Exception
	{
		sharedSecret = Preferences.loadFromDefaultLocation().getString(SHARED_SECRET_KEY);
	}

	/** Test communication with a local BlockStore instance. */
	@Test public void testLocalStorage() throws Throwable
	{
		Store store = new BlockStoreClient(
				new URL("http://localhost:8080"),
				new URL("http://localhost:8080/UploadManager/upload"),
				sharedSecret);

		Block b = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 }))
			.build();

		try
		{
			store.store(b);
			store.flush();
			assertEquals(b.getBytes(), store.retrieve(b.name()));
		}
		catch (ConnectException e)
		{
			Logger.getAnonymousLogger().warning(
				"Failed to connect to local upload server; is Tomcat running?");
		}
	}

	/** Test communication with the test server. */
	@Test public void testRemoteStorage() throws Throwable
	{
		if (sharedSecret.isEmpty())
			fail("Blockstore shared secret ('" + SHARED_SECRET_KEY + "') not set");

		Store store = new BlockStoreClient(
				new URL(BLOCKSTORE_DOWNLOAD_HOST),
				new URL(BLOCKSTORE_UPLOAD_URL),
				sharedSecret);

		Block b = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 }))
			.build();

		try
		{
			store.store(b);
			store.flush();
			assertEquals(b.getBytes(), store.retrieve(b.name()));
		}
		catch (UnknownHostException e)
		{
			Logger.getAnonymousLogger().warning(
				"Failed to resolve blockstore host; not connected to Internet?");
		}
		catch (SSLHandshakeException e)
		{
			Logger.getAnonymousLogger().severe(
				"SSL handshake error; problem with server certificate?");
		}
	}

	/** Where we download blocks. */
	private static final String BLOCKSTORE_DOWNLOAD_HOST =
		"https://s3-eu-west-1.amazonaws.com/me.footlights.userdata";

	/** Where to upload blocks. */
	private static final String BLOCKSTORE_UPLOAD_URL = "https://upload.footlights.me/upload";

	/** What the shared secret is called in the config file. */
	private static final String SHARED_SECRET_KEY = "blockstore.secret";

	/** For the moment, the server just checks for a shared secret */
	private static String sharedSecret;
}

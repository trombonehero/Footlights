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
package me.footlights.core.data.store;

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
		sharedSecret = Preferences.loadFromDefaultLocation().getString(SHARED_SECRET_KEY).get();
	}

	/** Test communication with a local BlockStore instance. */
	@Test public void testLocalStorage() throws Throwable
	{
		Store store = BlockStoreClient.newBuilder()
			.setDownloadURL(new URL("http://localhost:8080"))
			.setUploadURL(new URL("http://localhost:8080/UploadManager/upload"))
			.setSecretKey(sharedSecret)
			.build();

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

		Store store = BlockStoreClient.newBuilder()
			.setDownloadURL(new URL(BLOCKSTORE_DOWNLOAD_HOST))
			.setUploadURL(new URL(BLOCKSTORE_UPLOAD_URL))
			.setSecretKey(sharedSecret)
			.build();

		Block b = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 }))
			.build();

		try
		{
			store.store(b);
			store.flush();
			assertEquals(b, Block.parse(store.retrieve(b.name())));
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

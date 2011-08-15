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

import static org.junit.Assert.*;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import me.footlights.core.data.*;


public class FetcherTest
{
	@Before public void setUp() throws Exception
	{
		store = DiskStore.newBuilder()
			.createTemporaryDirectory()
			.build();

		fetcher = new Fetcher(store);
	}

	@Test public void testStoreAndFetch() throws Throwable
	{
		Block b = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }))
			.build();

		final String name = b.name();

		Link link = Link.newBuilder()
			.setUri(URI.create(name))
			.build();

		store.store(b);

		Fetcher.FetchResult fetched = fetcher.fetch(link);
		Map.Entry<String, Block> entry =
			fetched.plaintextBlocks.entrySet().iterator().next();

		assertEquals(name, entry.getKey());
		assertEquals(b, entry.getValue());
	}

	@Test public void testLinkedPlaintext() throws Throwable
	{
		
	}

	@Test public void testLinkedCiphertext() throws Throwable
	{
		/*
		Block b1 = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }))
			.build();

		Block b2 = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 }))
			.build();

		ByteBuffer e2 = b2.encrypt();

		b1.link(e2.name(), e2.algorithm(), e2.key().getEncoded());
		EncryptedBlock e1 = new EncryptedBlock(b1);

		store.store(e1);
		store.store(e2);

		Keychain keychain = new Keychain();
		keychain.addDecryptionKey(e1.algorithm(), e1.key(), e1.iv());

		Fetcher fetcher = new Fetcher(store);
		Set<Block> blocks = fetcher.fetch(e1.name(), keychain);
		assertEquals(2, blocks.size());
		*/

/*		List<Link> links = blocks.links();
		assertEquals(1, links.size());
		assertEquals(e2.name(), links.get(0).id());*/
	}

	private Store store;
	private Fetcher fetcher;
}

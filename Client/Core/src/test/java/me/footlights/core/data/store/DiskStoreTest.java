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

import java.nio.ByteBuffer;
import java.util.List;

import me.footlights.core.data.*;
import me.footlights.core.data.store.DiskStore;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;


public class DiskStoreTest
{
	@Before public void setUp() throws Throwable
	{
		store = DiskStore.newBuilder()
				.createTemporaryDirectory()
				.setCache(null)
				.build();
	}

	@Test public void testClearStorage() throws Throwable
	{
		Block b1 = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3 }))
			.build();

		store.store(b1);
		store.flush();

		assertEquals(b1.getBytes(), store.retrieve(b1.name()));
	}

	@Test public void testStoreAndFetch() throws Throwable
	{
		byte[] content = new byte[] { 1, 2, 3, 4 };
		Block b = Block.newBuilder().setContent(ByteBuffer.wrap(content)).build();
		store.store(b);

		Block retrieved = Block.parse(store.retrieve(b.name()));
		assertEquals(b, retrieved);
	}

	@Test public void testStoreFetchFile() throws Throwable
	{
		List<ByteBuffer> data = Lists.newLinkedList();
		data.add(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 }));
		data.add(ByteBuffer.wrap(new byte[] { 6, 7, 8, 9, 10 }));

		File f = File.newBuilder()
			.setContent(data)
			.freeze();

		store.store(f.toSave());

		File fetched = store.fetch(f.link());
		assertEquals(f, fetched);
	}

	private DiskStore store;
}


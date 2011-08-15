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

import me.footlights.core.data.*;
import me.footlights.core.data.store.DiskStore;
import me.footlights.core.data.store.Store;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class DiskStoreTest
{
	@Test public void testClearStorage() throws Throwable
	{
		Store store = DiskStore.newBuilder()
			.createTemporaryDirectory()
			.setCache(null)
			.build();

		Block b1 = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3 }))
			.build();

		store.store(b1);
		store.flush();

		assertEquals(b1.getBytes(), store.retrieve(b1.name()));
	}
}


package me.footlights.core.data.store;

import java.nio.ByteBuffer;

import me.footlights.core.data.Block;
import me.footlights.core.data.store.MemoryStore;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class MemoryStoreTest
{
	@Before public void setUp() { store = new MemoryStore(); }

	@Test public void testStoreAndRetrieve() throws Throwable
	{
		Block b = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4}))
			.build();

		store.store(b);

		assertEquals(b, Block.parse(store.retrieve(b.name())));
	}
	
	@Test public void testReplacement() throws Throwable
	{
		// TODO: LRU or something
	}

	private MemoryStore store;
}

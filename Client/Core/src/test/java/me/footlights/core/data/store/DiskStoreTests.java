package me.footlights.core.data.store;

import java.nio.ByteBuffer;

import me.footlights.core.data.*;
import me.footlights.core.data.store.DiskStore;
import me.footlights.core.data.store.Store;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class DiskStoreTests
{
	@Test public void testClearStorage() throws Throwable
	{
		Store store = new DiskStore();

		Block b1 = Block.newBuilder()
			.setContent(ByteBuffer.wrap(new byte[] { 1, 2, 3 }))
			.build();

		store.store(b1);
		store.flush();

		assertEquals(b1.getBytes(), store.retrieve(b1.name()));
	}
}


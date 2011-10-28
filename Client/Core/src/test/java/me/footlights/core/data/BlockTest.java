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
package me.footlights.core.data;

import java.net.URI;
import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

import me.footlights.core.Util;
import me.footlights.core.data.Block;
import me.footlights.core.data.FormatException;


public class BlockTest
{
	@Test public void testSetContent() throws Throwable
	{
		byte[] orig = new byte[] { 1, 2, 3, 4 };
		Block b = Block.newBuilder().setContent(ByteBuffer.wrap(orig)).build();

		byte[] copy = new byte[orig.length];
		b.content().get(copy);

		assertArrayEquals(orig, copy);
	}

	@Test public void testParsing() throws FormatException
	{
		Link link = Link.newBuilder().setUri(URI.create("foo")).build();

		ByteBuffer data = ByteBuffer.allocate(64);
		Util.setByteOrder(data);

		data.put(new byte[] { (byte) 0xF0, 0x07, (byte) 0xDA, 0x7A, '\r', '\n' });
		data.put((byte) 6);                 // total size: 2^6 = 64
		data.put((byte) 1);                 // 1 link
		data.putInt(16 + link.bytes());     // user data offset

		ByteBuffer content = ByteBuffer.allocate(16);
		data.putInt(content.capacity());
		for (int i = 0; i < content.capacity(); i++) content.put((byte) i);
		content.flip();

		data.put(link.getBytes());
		data.put(content);

		ByteBuffer padding = ByteBuffer.allocate(data.remaining());
		data.put(padding);

		data.flip();
		Block block = Block.parse(data);

		assertEquals(64, block.bytes());

		assertEquals(1, block.links().size());
		assertEquals(link, block.links().get(0));

		assertEquals(content.capacity(), block.content().limit());
		for (int i = 0; i < content.capacity(); i++)
			assertEquals(content.get(i), block.content().get(i));
	}

	@Test public void testGenerateAndParse() throws FormatException
	{
		Block.Builder builder = Block.newBuilder();

		builder.setContent(ByteBuffer.wrap(new byte[16]));
		builder.addLink(Link.newBuilder()
			.setAlgorithm("ABC/DEF-128")
			.setUri(URI.create("foo_bar"))
			.build());

		builder.addLink(Link.newBuilder()
			.setAlgorithm("JON-4096")
			.setUri(URI.create("http://foo.com/bar_server?id=baz"))
			.build());

		Block original = builder.build();
		Block parsed = Block.parse(original.getBytes());

		assertEquals(original, parsed);
	}

	@Test public void blockSizes() throws Throwable
	{
		final int[] acceptable = { 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 65536 };
		final int[] unacceptable = { 15, 17, 31 };

		for (int size : acceptable)
		{
			Block b = Block.newBuilder().setDesiredSize(size).build();
			assertEquals(size, b.bytes());
		}

		for (int size : unacceptable)
		{
			try
			{
				Block.newBuilder().setDesiredSize(size).build();
				fail("We should not be able to build a " + size + "B Block");
			}
			catch (FormatException e) { /* correct error */ }
		}
	}
}

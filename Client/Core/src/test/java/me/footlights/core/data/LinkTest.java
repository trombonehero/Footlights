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

import me.footlights.core.data.FormatException;
import me.footlights.core.data.Link;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Unit tests for {@link Link}.
 * @author Jonathan Anderson (jon@footlights.me)
 */
public class LinkTest
{
	/** Ensure that the simplest legal {@link Link} can be built. */
	@Test public void buildTrivialLink() throws FormatException
	{
		Link.newBuilder().setUri(URI.create("foo")).build();
	}

	/** Try to build a simpler-than-is-allowed {@link Link}. */
	@Test public void buildDegenerateLink()
	{
		try
		{
			Link.newBuilder().build();
			fail("URI-less Link should not build");
		}
		catch (IllegalArgumentException e) {}
	}

	/** Test parsing a very simple {@link Link}. */
	@Test public void parseSimpleLink() throws FormatException
	{
		// 256b key (32B)
		final byte[] key =
		{
				1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8,
				1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8,
		};

		final byte[] bytes = new byte[]
	   {
			'L', 'I', 'N', 'K', '\r', '\n',       // magic
			0x27, 0x00,                           // length: 39 (0x0027)
			0, 0,                                 // algorithm unspecified
			0x07, 0x00,                           // ID length: 7
			0x00, 0x01,                           // key length: 256b (0x0100)
			0, 0,                                 // IV unspecified
			's', 'o', 'm', 'e', '_', 'I', 'D',    // ID: "some ID"
		};
		
		ByteBuffer buffer = ByteBuffer.allocate(bytes.length + key.length)
			.put(bytes)
			.put(key);
		buffer.position(0);    // reset to beginning

		final String defaultAlgorithm = "FOO";
		final URI uri = URI.create("some_ID");

		Link l = Link.newBuilder()
			.setAlgorithm(defaultAlgorithm)
			.parse(buffer)
			.build();

		assertEquals(defaultAlgorithm, l.algorithm());
		assertEquals(uri, l.uri());
		assertEquals(0, l.ivLength());
		assertArrayEquals(key, l.key());
	}

	/** Test parsing a {@link Link} with fewer bytes than advertised. */
	@Test public void parseTooSmall() throws FormatException
	{
		final byte[] bytes = new byte[]
	   {
			'L', 'I', 'N', 'K', '\r', '\n',       // magic
			0x27, 0x00,                           // length: 39 (0x0027)
			0, 0,                                 // algorithm unspecified
			0x07, 0x00,                           // ID length: 7
			0x00, 0x01,                           // key length: 256b (0x0100)
			0, 0,                                 // IV unspecified
			's', 'o', 'm', 'e', '_', 'I', 'D',    // ID: "some ID"
		};

		try
		{
			Link.newBuilder().parse(ByteBuffer.wrap(bytes)).build();
			fail("Parsing a too-small ByteBuffer should fail");
		}
		catch (FormatException e)
		{
			if (!e.getMessage().contains("too small")) throw e;
		}
	}

	/** Test {@link Link#getBytes()}. */
	@Test public void exportLink() throws FormatException
	{
		final String algorithms = "FOO/BAR-128";
		final byte[] algBytes = new byte[] {
			'F', 'O', 'O', '/', 'B', 'A', 'R', '-', '1', '2', '8'
		};

		final URI uri = URI.create("some_ID");
		final byte[] uriBytes = new byte[] {
			's', 'o', 'm', 'e', '_', 'I', 'D'
		};

		// 256b key (32B)
		final byte[] key =
		{
				1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8,
				1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8,
		};

		Link l = Link.newBuilder()
			.setAlgorithm(algorithms)
			.setUri(uri)
			.setKey(key)
			.build();

		ByteBuffer buffer = l.getBytes();

		byte[] expectedMagic = new byte[] { 'L', 'I', 'N', 'K', '\r', '\n' };
		byte[] magic = new byte[6];
		buffer.get(magic);
		assertArrayEquals(expectedMagic, magic);

		final short expectedContentLength = (short)
			(algorithms.length()
			 + uri.toString().length()
			 + key.length);

		assertEquals(expectedContentLength, buffer.getShort());
		assertEquals((short) algorithms.length(), buffer.getShort());
		assertEquals((short) uri.toString().length(), buffer.getShort());
		assertEquals((short) 8 * key.length, buffer.getShort());
		assertEquals(0, buffer.getShort());

		byte[] tmp = new byte[algBytes.length];
		buffer.get(tmp);
		assertArrayEquals(algBytes, tmp);

		tmp = new byte[uriBytes.length];
		buffer.get(tmp);
		assertArrayEquals(uriBytes, tmp);

		tmp = new byte[key.length];
		buffer.get(tmp);
		assertArrayEquals(key, tmp);
	}

	@Test public void serializeAndDeserialize() throws FormatException
	{
		Link original = Link.newBuilder()
			.setAlgorithm("some algorithms")
			.setUri(URI.create("http://foo.com/bar?id=baz"))
			.setKey(new byte[] { 1, 2, 3, 4 })
			.build();

		Link copy = Link.parse(original.getBytes());
		assertEquals(original, copy);
	}
}

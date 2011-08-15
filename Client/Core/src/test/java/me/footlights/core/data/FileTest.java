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

import java.nio.ByteBuffer;
import java.util.List;

import me.footlights.core.data.File;

import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class FileTest
{
	@Test public void emptyFile() throws Throwable
	{
		File f = File.newBuilder().freeze();
		assertEquals(0, f.content().size());
		assertEquals(1, f.toSave().size());
	}

	@Test public void singleBlock() throws Throwable
	{
		byte[] rawData = new byte[] { 1, 2, 3, 4 };

		List<ByteBuffer> data = Lists.newArrayList();
		data.add(ByteBuffer.wrap(rawData));

		File f = File.newBuilder()
			.setContent(data)
			.freeze();

		assertEquals(1, f.content().size());
		assertEquals(2, f.toSave().size());

		byte[] dataOut = new byte[rawData.length];
		f.content().get(0).get(dataOut);
		assertArrayEquals(rawData, dataOut);
	}

	/** Ensure that a file, composed of several blocks, can be viewed as a seamless unit. */
	@Test public void seamlessView() throws Throwable
	{
		byte[] orig = new byte[64];
		for (byte i = 0; i < orig.length; i++) orig[i] = i;

		File f = File.newBuilder()
			.setContent(Lists.newArrayList(ByteBuffer.wrap(orig)))
			.setDesiredBlockSize(32)
			.freeze();

		assertTrue(f.content().size() > 1);

		byte[] copy = new byte[orig.length];
		int bytes = f.getInputStream().read(copy);
		assertEquals(orig.length, bytes);
		assertArrayEquals(orig, copy);
	}
}

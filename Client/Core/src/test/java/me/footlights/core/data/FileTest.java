package me.footlights.core.data;

import java.nio.ByteBuffer;
import java.util.List;

import me.footlights.core.data.File;

import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


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
		byte[] orig = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		ByteBuffer b1 = ByteBuffer.wrap(orig, 0, 3);
		ByteBuffer b2 = ByteBuffer.wrap(orig, 3, 5);
		List<ByteBuffer> data = Lists.newArrayList(b1, b2);

		File f = File.newBuilder().setContent(data).freeze();

		byte[] copy = new byte[orig.length];
		int bytes = f.getInputStream().read(copy);
		assertEquals(orig.length, bytes);
		assertArrayEquals(orig, copy);
	}
}

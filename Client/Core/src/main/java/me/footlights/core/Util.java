package me.footlights.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Util
{
	public static void setByteOrder(ByteBuffer buffer)
	{
		buffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	/** Load a URL's contents into a byte array */
	public static byte[] loadBytes(URL url) throws IOException
	{
		InputStream input = (InputStream) url.getContent();

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		while(true)
		{
			int length = input.read(buffer, 0, 1024);

			if(length < 0) break;
			else bytes.write(buffer, 0, length);
		}

		return bytes.toByteArray();
	}
}

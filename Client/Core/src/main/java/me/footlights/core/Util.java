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

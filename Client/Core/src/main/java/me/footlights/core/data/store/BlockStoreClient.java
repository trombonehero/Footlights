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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;

import me.footlights.core.data.*;
import me.footlights.core.data.store.Store;


/** A client for the Footlights BlockStore */
public final class BlockStoreClient extends Store
{
	@Override
	public ByteBuffer get(String name) throws IOException, NoSuchBlockException
	{
		URL fileUrl = new URL(downloadBase + "/" + URLEncoder.encode(name, "utf-8"));
		HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();

		switch (connection.getResponseCode())
		{
			case 200: break;
			case 410:
				throw new NoSuchBlockException(this, name);

			default:
				throw new IOException("Unknown server response " + connection.getResponseCode());
		}

		final String type = connection.getContentType();
		if (!type.equals("application/octet-stream"))
			throw new IOException("Unknown mime-type '" + type + "'");

		final int len = connection.getContentLength();
		ByteBuffer data = ByteBuffer.allocate(len);
		InputStream in = connection.getInputStream();

		byte[] buffer = new byte[1024];
		while (data.position() < len)
		{
			int read = in.read(buffer);
			if (read < 0)
				throw new IOException("Error reading block from server");
			data.put(buffer, 0, read);
		}

		data.flip();
		return data.asReadOnlyBuffer();
	}


	public static Builder newBuilder() { return new Builder(); }
	public static class Builder
	{
		public Builder setUploadURL(URL up)     { this.up = up;       return this; }
		public Builder setDownloadURL(URL down) { this.down = down;   return this; }
		public Builder setSecretKey(String key) { this.key = key;     return this; }
		public Builder setCache(Store cache)    { this.cache = cache; return this; }

		public BlockStoreClient build() throws MalformedURLException
		{
			return new BlockStoreClient(down, up, key, cache);
		}

		private URL down;
		private URL up;
		private String key;
		private Store cache;
	}

	@Override
	protected void put(String name, ByteBuffer bytes) throws IOException
	{
		URLConnection conn = uploadHost.openConnection();

		conn.setDoInput(true);
		conn.setDoOutput(true);

		final String CRLF = "\r\n";
		final String boundary = Long.toHexString(System.currentTimeMillis());
		conn.setRequestProperty("Content-Type",
				"multipart/form-data; boundary=" + boundary);


		OutputStream out = conn.getOutputStream();
		Writer writer = new PrintWriter(out, true);

		// TODO: DIGEST_ALGORITHM
		writer
			.append("--").append(boundary).append(CRLF)
			.append("Content-Disposition: form-data; name=\"AUTHENTICATOR\"").append(CRLF)
			.append("Content-Type: text/plain").append(CRLF)
			.append(CRLF)
			.append(key).append(CRLF)
			.flush()
			;

		writer
			.append("--").append(boundary).append(CRLF)
			.append("Content-Disposition: form-data; name=\"EXPECTED_NAME\"").append(CRLF)
			.append("Content-Type: text/plain").append(CRLF)
			.append(CRLF)
			.append(name).append(CRLF)
			.flush()
			;

		writer
			.append("--").append(boundary).append(CRLF)
			.append("Content-Disposition: form-data; name=\"FILE_CONTENTS\"; filename=\"upload\"").append(CRLF)
			.append("Content-Type: application/octet-stream").append(CRLF)
			.append("Content-Transfer-Encoding: binary").append(CRLF)
			.append(CRLF)
			.flush()
			;


		ByteBuffer copy = bytes.asReadOnlyBuffer();
		byte[] array = new byte[Math.min(4096, copy.remaining())];
		while (copy.hasRemaining())
		{
			int count = Math.min(copy.remaining(), array.length);
			copy.get(array, 0, count);
			out.write(array, 0, count);
		}
		out.flush();

		writer.append(CRLF).flush();
		writer.append("--").append(boundary).append("--").append(CRLF);

		writer.close();


		// get the HTTP response code
		BufferedReader reader =
			new BufferedReader(new InputStreamReader(conn.getInputStream()));

		String returnedName = reader.readLine();
		if (!returnedName.equals(name))
			throw new RuntimeException("Bad name: " + name + " != " + returnedName);
	}


	private BlockStoreClient(URL down, URL up, String key, Store cache)
		throws MalformedURLException
	{
		super(cache);

		this.downloadBase = down;
		this.uploadHost = up;
		this.key = key;
	}


	/** Location of the block store. */
	private URL downloadBase;
	private URL uploadHost;

	/** Capability to create blocks on the server. */
	private String key;
}

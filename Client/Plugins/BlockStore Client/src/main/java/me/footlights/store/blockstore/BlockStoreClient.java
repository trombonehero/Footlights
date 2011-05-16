package me.footlights.store.blockstore;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import me.footlights.core.data.*;
import me.footlights.core.data.store.Store;


/** A client for the Footlights BlockStore */
public final class BlockStoreClient extends Store
{
	public BlockStoreClient(String down, URL up, String key, Store cache)
		throws MalformedURLException
	{
		super(cache);

		this.downloadHost = down;
		this.uploadHost = up;
		this.key = key;
	}

	public BlockStoreClient(String down, URL up, String key) throws MalformedURLException
	{
		this(down, up, key, null);
	}


	@Override
	public ByteBuffer get(String name) throws IOException, NoSuchBlockException
	{
		URL fileUrl = new URL(downloadHost + "/" + name);
		InputStream in = fileUrl.openStream();

		BufferedReader r = new BufferedReader(new InputStreamReader(in));

		// get the HTTP response code, etc.
		int responseCode = -1;
		String contentType = "";
		int len = -1;

		while (true)
		{
			String header = r.readLine().trim();

			if (header.length() == 0)
				break;

			if (header.startsWith("HTTP/1."))
				responseCode = Integer.parseInt(header.substring(9, 12));

			else if (header.startsWith("Content-Type: "))
				contentType = header.substring(14).trim();

			else if (header.startsWith("Content-Length: "))
				len = Integer.parseInt(header.substring(16).trim());
		}
		r.mark(0);

		switch(responseCode)
		{
			case -1:
				throw new IOException("No HTTP response from block store server");

			case 200: break;
			case 410:
				throw new NoSuchBlockException(this, name);

			default:
				throw new IOException("Unknown server response " + responseCode);
		}

		if(!contentType.equals("application/encrypted-blob"))
			throw new IOException("Unknown mime-type '" + contentType + "'");

		if (len <= 0)
			throw new IOException("Content-Length not specified");

		ByteBuffer buffer = ByteBuffer.allocate(len);
		in.read(buffer.array());

		return buffer.asReadOnlyBuffer();
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


	/** Location of the block store. */
	private String downloadHost;
	private URL uploadHost;

	/** Capability to create blocks on the server. */
	private String key;
}

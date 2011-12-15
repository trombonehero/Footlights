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

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

import me.footlights.api.ajax.JSON;
import me.footlights.core.crypto.Fingerprint;
import me.footlights.core.crypto.Keychain;
import me.footlights.core.crypto.SecretKey;
import me.footlights.core.data.File;
import me.footlights.core.data.Link;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class ResolverTest
{
	public ResolverTest() throws Throwable
	{
		f = Fingerprint.decode("sha-256:JA6RDME6RLZPILMRNRBD5AX5SDGMSHAJ2WHZKBWYIPLHFPODHHRQ====");
	}

	@Before public void setUp() throws Exception
	{
		io = Mockito.mock(IO.class);
		keychain = Mockito.mock(Keychain.class);
		url = new URL("http://127.0.0.1/foo/bar");

		resolver = new Resolver(io, keychain);
	}

	@Test public void testUrlWithKey() throws Throwable
	{
		JSON json = JSON.newBuilder()
			.put("fingerprint", f.encode())
			.put("key", "AES:00000000000000000000000000000000")
			.build();

		File mockFile = mockFile(json);
		when(io.fetch(url)).thenReturn(mockFile);

		Link link = resolver.resolve(url);
		assertEquals(Link.newBuilder()
				.setFingerprint(f)
				.setKey(SecretKey.newGenerator()
						.setAlgorithm("AES")
						.setBytes(new byte[16])
					.generate())
			.build(), link);
	}

	@Test public void testUrlWithNoKey() throws Throwable
	{
		JSON json = JSON.newBuilder()
			.put("fingerprint", "sha-256:JA6RDME6RLZPILMRNRBD5AX5SDGMSHAJ2WHZKBWYIPLHFPODHHRQ====")
			.build();

		File mockFile = mockFile(json);
		when(io.fetch(url)).thenReturn(mockFile);

		Link l = Mockito.mock(Link.class);
		when(keychain.getLink(f)).thenReturn(l);

		Link link = resolver.resolve(url);
		assertEquals(l, link);
	}

	private File mockFile(JSON json) throws IOException
	{
		File f = Mockito.mock(File.class);
		when(f.getContents()).thenReturn(ByteBuffer.wrap(json.toString().getBytes()));

		return f;
	}

	private final Fingerprint f;
	private URL url;

	private Resolver resolver;

	private @Mock IO io;
	private @Mock Keychain keychain;
}

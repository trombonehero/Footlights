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
package me.footlights.core.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


public class KeychainTest
{
	@Before public void setUp() throws Throwable
	{
		keychain = new Keychain();
		keychain.store(fingerprint,
				SigningIdentity.newGenerator()
					.setPrincipalName("test user")
					.generate());
	}

	@Test public void testGenerate() throws Throwable
	{
		// TODO: test en/decryption
	}

	@Test public void testExportImport() throws Throwable
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		keychain.exportKeystoreFile(out);

		Keychain copy = new Keychain();
		copy.importKeystoreFile(new ByteArrayInputStream(out.toByteArray()));

		assertEquals(keychain, copy);
	}

/*
	@Test public void foo() throws Throwable
	{
		String algorithm = "RSA/ECB/OAEPwithSHA-512andMGF1padding";
		Cipher e = Cipher.getInstance(algorithm);
		e.init(Cipher.ENCRYPT_MODE, keychain.publicKey());

		ByteArrayOutputStream ciphertext = new ByteArrayOutputStream();
		OutputStream estream = new CipherOutputStream(ciphertext, e);

		final String testInput = "Hello, world!";

		Writer out = new OutputStreamWriter(estream);
		out.write(testInput);
		out.close();


		Cipher d = Cipher.getInstance(algorithm);
		d.init(Cipher.DECRYPT_MODE, keychain.privateKey.getPrivateKey());

		ByteArrayOutputStream plaintext = new ByteArrayOutputStream();
		OutputStream dstream = new CipherOutputStream(plaintext, d);


		dstream.write(ciphertext.toByteArray());
		dstream.close();

		assertEquals(testInput, new String(plaintext.toByteArray()));
	}
*/

	private Keychain keychain;

	private static final KeyPair keyPair;
	private static final Fingerprint fingerprint;

	static
	{
		try { keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair(); }
		catch (NoSuchAlgorithmException e) { throw new Error(e); }

		fingerprint = Fingerprint.newBuilder()
			.setContent(keyPair.getPublic().getEncoded())
			.build();
	}
}

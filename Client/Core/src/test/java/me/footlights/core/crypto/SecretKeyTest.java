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

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;

import javax.crypto.Cipher;

import me.footlights.core.crypto.SecretKey.CipherBuilder;
import me.footlights.core.crypto.SecretKey.Operation;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import static org.junit.Assert.*;


/** Tests {@see SecretKey}. */
public class SecretKeyTest
{
	/** Make sure we generate something that looks like a key. */
	@Test public void testGeneration() throws Throwable
	{
		SecretKey k = SecretKey.newGenerator().generate();
		assertTrue(k.keySpec.getEncoded().length > 0);
	}

	/**
	 * Test encryption and decryption using test vectors from
	 * @url http://csrc.nist.gov/groups/STM/cavp/documents/aes/KAT_AES.zip.
	 */
	@Test public void testEncryptDecrypt() throws Throwable
	{
		for (String[] v : TEST_VECTORS)
		{
			int i = 0;

			String algorithm = v[i++];
			String mode = v[i++];
			byte[] secret = Hex.decodeHex(v[i++].toCharArray());
			short ivLength = (short) (4 * v[i++].length());
			byte[] plaintext = Hex.decodeHex(v[i++].toCharArray());
			byte[] ciphertext = Hex.decodeHex(v[i++].toCharArray());

			SecretKey key = SecretKey.newGenerator()
				.setAlgorithm(algorithm)
				.setBytes(secret)
				.generate();

			CipherBuilder builder = 
				key.newCipherBuilder()
					.setMode(mode)
					.setIvLength(ivLength);

			try
			{
				Cipher e = builder.setOperation(Operation.ENCRYPT).build();
				Cipher d = builder.setOperation(Operation.DECRYPT).build();

				assertArrayEquals(ciphertext, e.doFinal(plaintext));
				assertArrayEquals(plaintext, d.doFinal(ciphertext));
			}
			catch (InvalidKeyException e)
			{
				fail("Unable to construct an AES cipher with a " + (8 * secret.length)
					+ "-bit key; is the JCE unlimited strength policy installed?");
			}
		}
	}

	@Test public void encryptAndDecrypt() throws Throwable
	{
		final String[] vector = TEST_VECTORS[1];

		int i = 0;
		String algorithm = vector[i++];
		String mode = vector[i++];
		byte[] keyBytes = Hex.decodeHex(vector[i++].toCharArray());
		short ivlen = (short) (vector[i++].length() / 2);

		final String fullAlgorithm = algorithm + "/" + mode + "/NOPADDING";

		SecretKey key = SecretKey.newGenerator()
			.setAlgorithm(algorithm)
			.setBytes(keyBytes)
			.generate();

		Cipher encryptor = key
			.newCipherBuilder()
			.parseAlgorithm(fullAlgorithm)
			.setOperation(Operation.ENCRYPT)
			.setIvLength((short) (8 * keyBytes.length))
			.build();

		Cipher decryptor = key
			.newCipherBuilder()
			.parseAlgorithm(fullAlgorithm)
			.setOperation(Operation.DECRYPT)
			.setIvLength((short) (8 * keyBytes.length))
			.build();

		ByteBuffer plaintext = ByteBuffer.wrap(new byte[128]);
		ByteBuffer ciphertext = ByteBuffer.allocate(plaintext.remaining());
		ByteBuffer decrypted = ByteBuffer.allocate(plaintext.remaining());

		encryptor.doFinal(plaintext, ciphertext);
		decryptor.doFinal(ciphertext, decrypted);

		assertArrayEquals(plaintext.array(), decrypted.array());
	}


	private static final String[][] TEST_VECTORS =
	{
		{
			"AES", "ECB",
			"80000000000000000000000000000000",
			"",
			"00000000000000000000000000000000",
			"0edd33d3c621e546455bd8ba1418bec8",
		},
		{
			"AES", "CBC",
			"8000000000000000000000000000000000000000000000000000000000000000",
			"00000000000000000000000000000000",
			"00000000000000000000000000000000",
			"e35a6dcb19b201a01ebcfa8aa22b5759",
		},
	};
}

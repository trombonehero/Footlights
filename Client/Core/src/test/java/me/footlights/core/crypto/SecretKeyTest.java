package me.footlights.core.crypto;

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
		String[][] vectors =
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

		for (String[] v : vectors)
		{
			int i = 0;

			String algorithm = v[i++];
			String mode = v[i++];
			byte[] secret = Hex.decodeHex(v[i++].toCharArray());
			byte[] iv = Hex.decodeHex(v[i++].toCharArray());
			byte[] plaintext = Hex.decodeHex(v[i++].toCharArray());
			byte[] ciphertext = Hex.decodeHex(v[i++].toCharArray());

			SecretKey key = SecretKey.newGenerator()
				.setAlgorithm(algorithm)
				.setBytes(secret)
				.generate();

			CipherBuilder builder =
				key.newCipherBuilder()
					.setMode(mode)
					.setInitializationVector(iv);

			Cipher e = builder.setOperation(Operation.ENCRYPT).build();
			Cipher d = builder.setOperation(Operation.DECRYPT).build();

			assertArrayEquals(ciphertext, e.doFinal(plaintext));
			assertArrayEquals(plaintext, d.doFinal(ciphertext));
		}
	}
}

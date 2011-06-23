package me.footlights.core.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


public class KeychainTests
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

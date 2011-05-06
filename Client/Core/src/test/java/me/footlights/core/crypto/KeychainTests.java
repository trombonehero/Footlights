package me.footlights.core.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


public class KeychainTests
{
	@Before public void setUp() throws Throwable
	{
		keychain = new Keychain();
		privateKey = PrivateKey.newGenerator().setPrincipalName("test user").generate();
		publicKey = privateKey.publicKey();
	}

	@Test public void testGenerate() throws Throwable
	{
		Certificate certChain[] = privateKey.getCertificateChain();
		assertEquals("Cert should be self-signed (chain should have length 1)",
		             1, certChain.length);

		try { certChain[0].verify(publicKey); }
		catch(SignatureException e) { fail("Cert should verify public key"); }

		// TODO: test en/decryption
	}

	@Test public void testExportImport() throws Throwable
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		keychain.exportKeystoreFile(out);

		Keychain copy = new Keychain();
		copy.importKeystoreFile(new ByteArrayInputStream(out.toByteArray()));

		assertEquals(keychain.publicKeys(), copy.publicKeys());
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
	private PrivateKey privateKey;
	private PublicKey publicKey;
}

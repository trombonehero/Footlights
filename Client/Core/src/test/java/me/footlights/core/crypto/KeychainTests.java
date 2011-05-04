package me.footlights.core.crypto;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


public class KeychainTests
{
	@Before public void setUp() throws Throwable
	{
		keychain = new Keychain(File.createTempFile("tmp", ".keychain"), "");
	}

	@Test public void testGenerate() throws Throwable
	{
		PublicKey publicKey = keychain.publicKey;
		PrivateKey privateKey = keychain.privateKey.getPrivateKey();
		java.security.cert.Certificate certChain[] =
			keychain.privateKey.getCertificateChain();

		assertNotNull("Public key should not be null", publicKey);
		assertNotNull("Private key should not be null", privateKey);
		assertNotNull("Cert chain should not be null", certChain);

		assertEquals("Cert should be self-signed (chain should have length 1)",
		             1, certChain.length);

		try { certChain[0].verify(publicKey); }
		catch(SignatureException e) { fail("Cert should verify public key"); }
	}


	@Test public void testLoad() throws Throwable
	{
		PublicKey publicKey = keychain.publicKey;
		PrivateKey privateKey = keychain.privateKey.getPrivateKey();
		java.security.cert.Certificate certChain[] =
			keychain.privateKey.getCertificateChain();

		assertNotNull("Public key should not be null", publicKey);
		assertNotNull("Private key should not be null", privateKey);
		assertNotNull("Cert chain should not be null", certChain);

		assertEquals("Cert should be self-signed (chain should have length 1)",
		             1, certChain.length);

		try { certChain[0].verify(publicKey); }
		catch(SignatureException e) { fail("Cert should verify public key"); }
		

		byte expected[] =
		{
				-124,   74,  -25,   38,   89,   75,   24, -106,
				  67,  -48,   53,  -63,  -32,  115,   81,  -92,
				  89,   40,  121,  -24
		};

		byte[] fingerprint =
			MessageDigest.getInstance("SHA1").digest(publicKey.getEncoded());

		assertArrayEquals(expected, fingerprint);

		keychain.finalize();
		keychain = null;
	}


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


	private Keychain keychain;
}

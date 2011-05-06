package me.footlights.core.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.spec.SecretKeySpec;

import me.footlights.core.Config;


/** A secret, symmetric key, whose bits cannot be extracted outside of me.footlights.core.crypto. */
public class SecretKey
{
	Fingerprint getFingerprint() { return fingerprint; }

	public static Generator newGenerator() { return new Generator(); }
	public static class Generator
	{
		public Generator setAlgorithm(String a) { algorithm = a; return this; }
		public Generator setBytes(byte[] s) { secret = s; return this; }
		public Generator setFingerprintAlgorithm(String a) throws NoSuchAlgorithmException
		{
			fingerprint.setAlgorithm(a);
			return this;
		}

		public SecretKey generate() throws NoSuchAlgorithmException
		{
			if (secret == null)
			{
				AlgorithmFactory.CipherBuilder builder = AlgorithmFactory.newSymmetricCipherBuilder();
				builder.setCipherName(algorithm);

				secret = new byte[builder.getKeySize()];
				SecureRandom.getInstance(config.get("crypto.prng")).nextBytes(secret);
			}

			return new SecretKey(
					new SecretKeySpec(secret, algorithm),
					fingerprint.setContent(secret).build());
		}

		private Config config = Config.getInstance();

		private String algorithm = config.get("crypto.sym.algorithm");
		private byte[] secret = null;
		private Fingerprint.Builder fingerprint = Fingerprint.newBuilder();
	}

	private SecretKey(SecretKeySpec key, Fingerprint fingerprint)
	{
		this.keySpec = key;
		this.fingerprint = fingerprint;
	}

	final SecretKeySpec keySpec;
	private final Fingerprint fingerprint;
}

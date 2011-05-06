package me.footlights.core.crypto;

import java.security.NoSuchAlgorithmException;

import javax.crypto.spec.SecretKeySpec;

import me.footlights.core.Config;


/** A secret, symmetric key, whose bits cannot be extracted outside of me.footlights.core.crypto. */
public class SecretKey
{
	Fingerprint getFingerprint() { return fingerprint; }

	public static Generator newGenerator() { return new Generator(); }
	public static class Generator
	{
		public Generator setCryptoAlgorithm(String a) { cryptoAlgorithm = a; return this; }
		public Generator setBytes(byte[] s) { secret = s; return this; }
		public Generator setFingerprintAlgorithm(String a) throws NoSuchAlgorithmException
		{
			fingerprint.setAlgorithm(a);
			return this;
		}

		public SecretKey generate() throws NoSuchAlgorithmException
		{
			if (secret == null)
				// TODO: generate random bytes
				;
/*
			AlgorithmFactory.CipherBuilder builder =
				AlgorithmFactory.newSymmetricCipherBuilder();

			Cipher cipher = builder
				.setCipherName(cryptoAlgorithm)
				.setKey(secret)
				.build();
*/

			return new SecretKey(
					new SecretKeySpec(secret, cryptoAlgorithm),
					fingerprint.setContent(secret).build());
		}

		private Config config = Config.getInstance();

		private Fingerprint.Builder fingerprint = Fingerprint.newBuilder();

		private byte[] secret = null;

		private String cryptoAlgorithm = config.get("crypto.sym.algorithm");
	}

	private SecretKey(SecretKeySpec key, Fingerprint fingerprint)
	{
		this.keySpec = key;
		this.fingerprint = fingerprint;
	}

	final SecretKeySpec keySpec;
	private final Fingerprint fingerprint;
}
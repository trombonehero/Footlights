package me.footlights.core.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import me.footlights.core.Preferences;


/** A secret, symmetric key, whose bits cannot be extracted outside of me.footlights.core.crypto. */
public class SecretKey
{
	public enum Operation
	{
		ENCRYPT(Cipher.ENCRYPT_MODE),
		DECRYPT(Cipher.DECRYPT_MODE);

		private Operation(int value) { this.value = value; }
		private int opcode() { return value; }

		private final int value;
	};

	public Fingerprint getFingerprint() { return fingerprint; }

	/** Generate a new secret key. */
	public static Generator newGenerator() { return new Generator(); }

	/** Get the cipher instance, which can be used for encrypting and decrypting data. */
	public CipherBuilder newCipherBuilder() { return new CipherBuilder(); }


	public static class Generator
	{
		public String getAlgorithm() { return algorithm; }
		public int getKeyLength() { return keylen; }

		public Generator setAlgorithm(String a) { algorithm = a; return this; }
		public Generator setBytes(byte[] s) { secret = s; return this; }
		public Generator setFingerprintAlgorithm(String a) throws NoSuchAlgorithmException
		{
			fingerprint.setAlgorithm(a);
			return this;
		}
		public Generator setKeyLength(int l) { keylen = l; return this; }

		public SecretKey generate() throws NoSuchAlgorithmException
		{
			if (secret == null)
			{
				secret = new byte[keylen];
				SecureRandom.getInstance(preferences.getString("crypto.prng")).nextBytes(secret);
			}

			return new SecretKey(
					new SecretKeySpec(secret, algorithm),
					fingerprint.setContent(secret).build());
		}


		private String algorithm = preferences.getString("crypto.sym.algorithm");
		private int keylen = preferences.getInt("crypto.sym.keylen");
		private byte[] secret = null;
		private Fingerprint.Builder fingerprint = Fingerprint.newBuilder();
	}
	


	public class CipherBuilder
	{
		public Cipher build()
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			       InvalidAlgorithmParameterException
		{
			String fullAlgorithm = keySpec.getAlgorithm() + "/" + mode + "/" + padding;
			Cipher cipher = Cipher.getInstance(fullAlgorithm);

			cipher.init(operation.opcode(), keySpec, iv);

			return cipher;
		}

		public CipherBuilder setOperation(Operation o)  { operation = o; return this; }
		public CipherBuilder setMode(String m)          { mode = m;      return this; }
		public CipherBuilder setPaddingScheme(String p) { padding = p;   return this; }

		public CipherBuilder setInitializationVector(byte[] iv)
		{
			if ((iv == null) || (iv.length == 0)) this.iv = null;
			else this.iv = new IvParameterSpec(iv);

			return this;
		}


		private CipherBuilder() {}

		private Operation operation = Operation.ENCRYPT;
		private String mode = preferences.getString("crypto.sym.mode");
		private String padding = preferences.getString("crypto.sym.padding");

		private IvParameterSpec iv;
	}

	

	private SecretKey(SecretKeySpec key, Fingerprint fingerprint)
	{
		this.keySpec = key;
		this.fingerprint = fingerprint;
	}

	final SecretKeySpec keySpec;
	private final Fingerprint fingerprint;

	/** Footlights-wide preferences. */
	private static Preferences preferences = Preferences.getDefaultPreferences();
}

package me.footlights.core.crypto;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import me.footlights.core.Config;


/**
 * Creates initialized algorithms (PK, SK and hash functions).
 * @author Jonathan Anderson (jon@footlights.me)
 */
public class AlgorithmFactory
{
	public static class CipherBuilder
	{
		public Cipher build() throws GeneralSecurityException
		{
			String fullAlgorithm = name + "/" + mode + "/" + padding;
			Cipher cipher = Cipher.getInstance(fullAlgorithm);

			int mode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
			cipher.init(mode, key, iv);

			return cipher;
		}

		public String getCipherName() { return name; }
		public int getKeySize() { return keyByteCount; }

		public CipherBuilder setCipherName(String name)
		{
			this.name = name;
			return this;
		}
		public CipherBuilder setKey(byte[] key)
		{
			this.key = new SecretKeySpec(key, name);
			return this;
		}
		public CipherBuilder setInitializationVector(byte[] iv)
		{
			if (iv == null) this.iv = null;
			else this.iv = new IvParameterSpec(iv);

			return this;
		}

		private CipherBuilder()
		{
			this.name = config.get("crypto.sym.cipher");
			this.mode = config.get("crypto.sym.mode");
			this.padding = config.get("crypto.sym.padding");
			this.keyByteCount =
				Integer.parseInt(Config.getInstance().get("crypto.sym.keysize"));
		}

		private Config config = Config.getInstance();

		private boolean encrypt;
		private String name;
		private String mode;
		private String padding;
		private int keyByteCount;
		private SecretKeySpec key;
		private IvParameterSpec iv;
	}

	public static CipherBuilder newSymmetricCipherBuilder()
	{
		return new CipherBuilder();
	}
}

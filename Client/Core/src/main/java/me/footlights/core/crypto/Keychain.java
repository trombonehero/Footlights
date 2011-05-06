package me.footlights.core.crypto;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import me.footlights.core.Config;


/** Stores crypto keys. */
public class Keychain
{
	public Map<String,PublicKey> publicKeys()
	{
		return Maps.transformValues(privateKeys, PRIVATE_TO_PUBLIC);
	}

	public void store(PrivateKey key)
	{
		String name = key.getFingerprint().encode();

		if (privateKeys.containsKey(name))
			assert Arrays.equals(
					key.key.getPrivateKey().getEncoded(),
					privateKeys.get(name).key.getPrivateKey().getEncoded());

		privateKeys.put(name, key);
	}

	public void store(SecretKey key)
	{
		String name = key.getFingerprint().encode();

		if (secretKeys.containsKey(name))
			assert Arrays.equals(
					key.keySpec.getEncoded(),
					secretKeys.get(name).keySpec.getEncoded());

		secretKeys.put(name, key);
	}



	/** Merge a KeyStore file into this Keychain. */
	public void importKeystoreFile(InputStream input)
		throws CertificateException, IOException, KeyStoreException,
		       NoSuchAlgorithmException, UnrecoverableEntryException
	{
		importKeystoreFile(input, Config.getInstance().get("crypto.keystore.type"));
	}

	/** Merge a KeyStore file into this Keychain. */
	public void importKeystoreFile(InputStream input, String type)
		throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException,
		       UnrecoverableEntryException
	{
		final String password = getPassword();

		KeyStore store = KeyStore.getInstance(type);
		store.load(input, password.toCharArray());

		final KeyStore.ProtectionParameter protection =
			new KeyStore.PasswordProtection(password.toCharArray());

		Enumeration<String> aliases = store.aliases();
		while (aliases.hasMoreElements())
		{
			final String alias = aliases.nextElement();
			final String[] parts = alias.split(ALIAS_SEPARATOR);
			if (parts.length != 3)
				throw new KeyStoreException("Expected 'type:algorithm:hash', got '" + alias + "'");

			final String entryType = parts[0].toUpperCase();
			final String fingerprintAlgorithm = parts[1];

			EntryType t;
			try { t = EntryType.valueOf(entryType); }
			catch (IllegalArgumentException e)
			{
				throw new KeyStoreException("Invalid keystore entry type '" + parts[0] + "'");
			}

			KeyStore.Entry entry = store.getEntry(alias, protection);
			switch (t)
			{
				case PRIVATE:
					KeyStore.PrivateKeyEntry privateKey = (KeyStore.PrivateKeyEntry) entry;
					store(
						new PrivateKey(
							privateKey,
							Fingerprint.newBuilder()
								.setAlgorithm(fingerprintAlgorithm)
								.setContent(privateKey.getCertificate().getPublicKey().getEncoded())
								.build()));
					break;

				case SECRET:
					javax.crypto.SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
					store(
						SecretKey.newGenerator()
							.setCryptoAlgorithm(secretKey.getAlgorithm())
							.setFingerprintAlgorithm(fingerprintAlgorithm)
							.setBytes(secretKey.getEncoded())
							.generate());
					break;
			}
		}
	}


	/** Save a Keychain to a KeyStore file. */
	void exportKeystoreFile(OutputStream out)
		throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException,
		       UnrecoverableEntryException
   {
		exportKeystoreFile(out, Config.getInstance().get("crypto.keystore.type"));
   }

	/** Save a Keychain to a KeyStore file. */
	void exportKeystoreFile(OutputStream out, String type)
		throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException,
		       UnrecoverableEntryException
	{
		final String password = getPassword();

		KeyStore store = KeyStore.getInstance(type);
		store.load(null, password.toCharArray());

		final KeyStore.ProtectionParameter protection =
			new KeyStore.PasswordProtection(password.toCharArray());

		for (PrivateKey key : privateKeys.values())
			store.setEntry("private:" + key.getFingerprint().encoded(), key.key, protection);

		for (final SecretKey secret : secretKeys.values())
			store.setEntry(
					"secret:" + secret.getFingerprint().encoded(),
					new KeyStore.SecretKeyEntry(secret.keySpec),
					protection);

		store.store(out, password.toCharArray());
	}




	/** Kinds of things that we store in a Java Keystore. */
	private enum EntryType
	{
		/** A certificate. */
		CERT,

		/** A private key for a public/private keypair. */
		PRIVATE,

		/** A secret, symmetric key. */
		SECRET,
	}


	/** Retrieve the keystore password from somewhere trustworthy (the user?). */
	private final String getPassword() { return "fubar"; }


	private static final String	ALIAS_SEPARATOR  = ":";
	
	private static final Function<PrivateKey, PublicKey> PRIVATE_TO_PUBLIC =
		new Function<PrivateKey, PublicKey>()
		{
			public PublicKey apply(PrivateKey priv) { return priv.publicKey(); }
		};


	/** Public/private keypairs. */
	private final Map<String, PrivateKey> privateKeys = Maps.newHashMap();

	/** Secret keys for decrypting blocks */
	private final Map<String, SecretKey> secretKeys = Maps.newHashMap();
}

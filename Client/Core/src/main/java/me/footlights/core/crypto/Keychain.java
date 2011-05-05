package me.footlights.core.crypto;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import me.footlights.core.Config;
import me.footlights.core.MissingParameterException;

import sun.security.x509.*;


/** Stores crypto keys. */
public class Keychain
{
	public Map<String,PublicKey> publicKeys()
	{
		return Maps.transformValues(privateKeys, PRIVATE_TO_PUBLIC);
	}

	public void store(PrivateKey key)
	{
		String name = key.getFingerprint().base64ish();

		if (privateKeys.containsKey(name))
			assert Arrays.equals(
					key.key.getPrivateKey().getEncoded(),
					privateKeys.get(name).key.getPrivateKey().getEncoded());

		privateKeys.put(name, key);
	}

	public void store(SecretKey key)
	{
		String name = key.getFingerprint().base64ish();

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
							.setSignatureAlgorithm(fingerprintAlgorithm)
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
	
	

	/** A private key, whose bits cannot be extracted outside of Keychain. */
	static class PrivateKey
	{
		public Fingerprint getFingerprint() { return fingerprint; }
		public PublicKey publicKey() { return certificate().getPublicKey(); }
		public Certificate certificate() { return key.getCertificate(); }
		public Certificate[] getCertificateChain()
		{
			Certificate[] certs = key.getCertificateChain();
			return Arrays.copyOf(certs, certs.length);
		}

		public static Generator newGenerator() { return new Generator(); }

		public static class Generator
		{
			public Generator setPrincipalName(String name) throws IOException
			{
				x500Name = new X500Name(name, ORG_UNIT, ORGANIZATION, LOCALITY);
				return this;
			}

			public Generator setPublicKeyType(String t) { publicKeyType = t; return this; }
			public Generator setSignatureAlgorithm(String a) { signAlgorithm = a; return this; }
			public Generator setKeyLength(int l) { keyLength = l; return this; }
			public Generator setValiditySeconds(int v) { validity = v; return this; }

			public PrivateKey generate()
				throws CertificateException, InvalidKeyException, MissingParameterException,
				       NoSuchAlgorithmException, NoSuchProviderException,
				       SignatureException
			{
				CertAndKeyGen gen = new CertAndKeyGen(publicKeyType, signAlgorithm);
				gen.generate(keyLength);

				if (x500Name == null) throw new MissingParameterException("principal name");

				X509Certificate cert = gen.getSelfCertificate(x500Name, validity);
				java.security.cert.Certificate chain[] = { cert };

				PrivateKey privateKey = new PrivateKey(
						new KeyStore.PrivateKeyEntry(gen.getPrivateKey(), chain),
						Fingerprint.newBuilder()
							.setAlgorithm(hashAlgorithm)
							.setContent(gen.getPublicKey().getEncoded())
							.build());

				return privateKey;
			}

			private Config config = Config.getInstance();

			private X500Name x500Name;

			private String publicKeyType = config.get("crypto.asym.algorithm");
			private String hashAlgorithm = config.get("crypto.hash.algorithm");
			private String signAlgorithm =
				hashAlgorithm.replaceAll("-", "") + "with" + publicKeyType;

			private int keyLength = config.getInt("crypto.asym.keylen");
			private int validity = config.getInt("crypto.cert.validity");
		}

		private PrivateKey(KeyStore.PrivateKeyEntry key, Fingerprint fingerprint)
		{
			this.key = key;
			this.fingerprint = fingerprint;
		}

		private final KeyStore.PrivateKeyEntry key;
		private final Fingerprint fingerprint;
	}


	/** A secret, symmetric key. */
	static class SecretKey
	{
		Fingerprint getFingerprint() { return fingerprint; }

		public static Generator newGenerator() { return new Generator(); }
		public static class Generator
		{
			public Generator setCryptoAlgorithm(String a) { cryptoAlgorithm = a; return this; }
			public Generator setBytes(byte[] s) { secret = s; return this; }
			public Generator setSignatureAlgorithm(String a) throws NoSuchAlgorithmException
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

		private final SecretKeySpec keySpec;
		private final Fingerprint fingerprint;
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

	private static final String	LOCALITY         = "The Internet";
	private static final String	ORGANIZATION     = "Footlights";
	private static final String	ORG_UNIT         = "Footlights Users";
	
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

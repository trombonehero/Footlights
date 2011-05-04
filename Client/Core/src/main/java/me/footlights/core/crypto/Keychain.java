package me.footlights.core.crypto;

import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.KeyStore.ProtectionParameter;
import java.security.cert.*;
import java.security.spec.*;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import com.google.common.collect.Lists;

import me.footlights.core.Config;

import sun.security.x509.*;

import static me.footlights.core.Util.loadBytes;


/** Stores crypto keys */
public class Keychain
{
	public static Keychain load(InputStream input, String password)
		throws CertificateException, IOException, KeyStoreException,
		       NoSuchAlgorithmException, UnrecoverableEntryException
	{
		return load(input, password,
			Config.getInstance().get("crypto.keystore.type"));
	}

	public static Keychain load(InputStream input, String password, String type)
		throws CertificateException, IOException, KeyStoreException,
		       NoSuchAlgorithmException, UnrecoverableEntryException
	{
		KeyStore store = KeyStore.getInstance(type);
		store.load(input, password.toCharArray());

		if (!store.containsAlias(DEFAULT_PRIVATE_KEY_NAME))
			throw new KeyNotFoundException(DEFAULT_PRIVATE_KEY_NAME);


		final KeyStore.ProtectionParameter protection =
			new KeyStore.PasswordProtection(password.toCharArray());

		KeyStore.PrivateKeyEntry privateKey =
			(KeyStore.PrivateKeyEntry)
			store.getEntry(DEFAULT_PRIVATE_KEY_NAME, protection);

		return new Keychain(store, privateKey, protection);
	}

	public static Keychain generate(String password)
		throws KeyStoreException
	{
		return generate(Config.getInstance().get("crypto.keystore.type"));
	}

	public static Keychain generate(String password, String type)
		throws KeyStoreException
	{
		KeyStore store = KeyStore.getInstance(type);
	}


	Keychain(KeyStore keyStore, KeyStore.PrivateKeyEntry privateKey,
	                final KeyStore.ProtectionParameter protection)
	{
		this.secretKeys = Lists.newArrayList();

		this.keyStore = keyStore;
		this.privateKey = privateKey;

		this.loadStore =
			new KeyStore.LoadStoreParameter() {
				public ProtectionParameter getProtectionParameter() {
					return protection;
				}
			};
	}


	/** Destructor */
	public void finalize() throws Throwable
	{
		if(dirty) keyStore.store(loadStore);
		super.finalize();
	}


	/** Generate an n-bit keypair */
	public PublicKey generate(
			String name, String algorithm, String signAlgorithm, int length)
		throws GeneralSecurityException
	{
		// no, we don't... generate one!
		X500Name x500Name;
		try
		{
			// TODO: config?
			x500Name = new X500Name(name, "Footlights Users",
			                        "Footlights", "The Internet");
		}
		catch(IOException e) { throw new Error(e); }


		CertAndKeyGen gen = new CertAndKeyGen(algorithm, signAlgorithm);
		gen.generate(length);

		PublicKey publicKey = gen.getPublicKey();

		int validity = Integer.parseInt(
				Config.getInstance().get("crypto.cert.validity"));

		X509Certificate cert = gen.getSelfCertificate(x500Name, validity);

		java.security.cert.Certificate chain[] = { cert };
		KeyStore.PrivateKeyEntry privateKey =
			new KeyStore.PrivateKeyEntry(gen.getPrivateKey(), chain);

		keyStore.setEntry(name, privateKey, loadStore.getProtectionParameter());
		dirty = true;

		return publicKey;
	}


	public PublicKey publicKey()
	{
		return privateKey.getCertificate().getPublicKey();
	}



	/** Load a public key from a URL */
	public PublicKey loadPublic(URL url, String algorithm)
		throws IOException, InvalidKeySpecException, NoSuchAlgorithmException
	{
		return loadPublic(loadBytes(url), algorithm);
	}


	/** Load a public key from a byte array */
	public PublicKey loadPublic(byte[] encoded, String algorithm)
		throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encoded);
		KeyFactory kf = KeyFactory.getInstance(algorithm);

		return kf.generatePublic(publicKeySpec);
	}



	/** Load a private key from a URL */
	public PrivateKey loadPrivate(URL url, String algorithm)
		throws IOException, InvalidKeySpecException, NoSuchAlgorithmException
	{
		return loadPrivate(loadBytes(url), algorithm);
	}


	/** Load a private key from a byte array */
	public PrivateKey loadPrivate(byte[] encoded, String algorithm)
		throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		PKCS8EncodedKeySpec publicKeySpec = new PKCS8EncodedKeySpec(encoded);
		KeyFactory kf = KeyFactory.getInstance(algorithm);

		return kf.generatePrivate(publicKeySpec);
	}
	
	
	public void addDecryptionKey(
		String algorithm, SecretKeySpec keySpec, byte[] iv)
	{
		// TODO: use the KeyStore
		SecretKey key = new SecretKey();
		key.algorithm = algorithm;
		key.keySpec = keySpec;
		key.iv = iv;

		secretKeys.add(key);
	}


	private static final String DEFAULT_PRIVATE_KEY_NAME = "root-private";


	/** How the keystore is protected (e.g. password). */
	private final KeyStore.LoadStoreParameter loadStore;

	/** JCA-provided key store */
	private final KeyStore keyStore;

	/** Our private keypair. */
	private final KeyStore.PrivateKeyEntry privateKey;

	/** Is the key chain dirty (needs to be written to disk)? */
	private boolean dirty;


	/** Holds a secret key and an IV */
	class SecretKey
	{
		public String algorithm;
		public SecretKeySpec keySpec;
		public byte[] iv;
	}

	/** Secret keys for decrypting blocks */
	private final List<SecretKey> secretKeys;
}

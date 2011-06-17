package me.footlights.core.crypto;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import me.footlights.core.Preferences;
import me.footlights.core.MissingParameterException;
import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;


/** A private key, whose bits cannot be extracted outside of me.footlights.core.crypto. */
public class PrivateKey
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


		/** Footlights-wide preferences. */
		private static Preferences preferences = Preferences.getDefaultPreferences();

		private X500Name x500Name;

		private String publicKeyType = preferences.getString("crypto.asym.algorithm");
		private String hashAlgorithm = preferences.getString("crypto.hash.algorithm");
		private String signAlgorithm =
			hashAlgorithm.replaceAll("-", "") + "with" + publicKeyType;

		private int keyLength = preferences.getInt("crypto.asym.keylen");
		private int validity = preferences.getInt("crypto.cert.validity");
	}

	PrivateKey(KeyStore.PrivateKeyEntry key, Fingerprint fingerprint)
	{
		this.key = key;
		this.fingerprint = fingerprint;
	}

	private static final String	LOCALITY         = "The Internet";
	private static final String	ORGANIZATION     = "Footlights";
	private static final String	ORG_UNIT         = "Footlights Users";

	final KeyStore.PrivateKeyEntry key;
	private final Fingerprint fingerprint;
}

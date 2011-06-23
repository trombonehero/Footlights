package me.footlights.core.crypto;

import java.security.PublicKey;
import java.security.cert.Certificate;


/** A self-declared identity, represented by a self-signed certificate. */
public class Identity
{
	public Certificate getCertificate() { return certificate; }
	public PublicKey publicKey() { return certificate.getPublicKey(); }

	protected Identity(Certificate certificate)
	{
		this.certificate = certificate;
	}

	@Override public boolean equals(Object other)
	{
		if (!(other instanceof Identity)) return false;
		Identity o = (Identity) other;

		return certificate.equals(o.certificate);
	}

	private final Certificate certificate;
}

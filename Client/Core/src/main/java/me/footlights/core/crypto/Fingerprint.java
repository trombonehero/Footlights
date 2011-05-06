package me.footlights.core.crypto;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

import com.google.common.annotations.VisibleForTesting;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import me.footlights.HasBytes;
import me.footlights.core.Config;
import me.footlights.core.ConfigurationError;


/** A fingerprint for a number of bytes. */
public class Fingerprint
{
	public static byte[] decode(String name) throws Base64DecodingException
	{
		return Base64.decode(name.replaceAll("+", "/"));
	}

	public static Builder newBuilder() { return new Builder(); }

	public String encode()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(algorithm.getAlgorithm().toLowerCase());
		sb.append(":");
		sb.append(Base64.encode(bytes.array()).replaceAll("/", "+"));

		return sb.toString();
	}

	public MessageDigest getAlgorithm() { return algorithm; }

	public boolean matches(ByteBuffer b) { return (0 == bytes.compareTo(b)); }
	public boolean matches(byte[] b) { return matches(ByteBuffer.wrap(b)); }

	public ByteBuffer getBytes() { return bytes.asReadOnlyBuffer(); }

	public static class Builder
	{
		public Fingerprint build()
		{
			ByteBuffer hash = ByteBuffer.wrap(algorithm.digest(bytes));
			return new Fingerprint(algorithm, hash);
		}

		public Builder setAlgorithm(String a) throws NoSuchAlgorithmException
		{
			algorithm = MessageDigest.getInstance(a);
			return this;
		}

		public Builder setContent(byte[] b) { bytes = b; return this; }
		public Builder setContent(HasBytes h) { return setContent(h.getBytes()); }
		public Builder setContent(ByteBuffer b)
		{
			if (b.isReadOnly())
			{
				bytes = new byte[b.remaining()];
				b.get(bytes);
			}
			else bytes = b.array();

			return this;
		}
		
		private Builder()
		{
			try
			{
				algorithm = MessageDigest.getInstance(
						Config.getInstance().get("crypto.hash.algorithm"));
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new ConfigurationError("Invalid hash algorithm: " + e);
			}
		}

		private MessageDigest algorithm;
		private byte[] bytes;
	}


	@VisibleForTesting String hex() { return Hex.encodeHexString(bytes.array()); }

	private Fingerprint(MessageDigest hashAlgorithm, ByteBuffer fingerprintBytes)
	{
		this.algorithm = hashAlgorithm;
		this.bytes = fingerprintBytes;
	}

	private MessageDigest algorithm;
	private ByteBuffer bytes;
}

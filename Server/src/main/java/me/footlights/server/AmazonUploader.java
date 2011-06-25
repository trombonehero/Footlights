package me.footlights.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import me.footlights.core.Preferences;
import me.footlights.core.ConfigurationError;
import me.footlights.core.crypto.Fingerprint;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;

import com.google.inject.Inject;


/**
 * Uploads user data blocks to Amazon S3.
 */
public final class AmazonUploader implements Uploader
{
	@Inject
	public AmazonUploader(Preferences preferences)
	{
		authSecret = preferences.getString("blockstore.secret");
		if (authSecret.isEmpty()) throw new ConfigurationError("BlockStore secret not set");

		final String keyId = preferences.getString("amazon.keyId");
		final String secret = preferences.getString("amazon.secretKey");

		if (keyId.isEmpty()) throw new ConfigurationError("Amazon key ID not set");
		if (secret.isEmpty()) throw new ConfigurationError("Amazon secret key not set");

		AWSCredentials cred = new AWSCredentials()
		{
			@Override public String getAWSAccessKeyId() { return keyId; }
			@Override public String getAWSSecretKey() { return secret; }
		};

		s3 = new AmazonS3Client(cred);
	}


	/**
	 * Upload file to Amazon S3, unless there's a reason not to:
	 *  - invalid request
	 *  - not authorized
	 *  - incorrect size
	 * 
	 * @return the name of the uploaded file (which is a fingerprint)
	 */
	@Override public String upload(final Block block)
		throws AccessControlException, IOException, NoSuchAlgorithmException, RuntimeException
	{
		log.entering(AmazonUploader.class.getName(), "upload", block);

		if (!checkAuth(block.getAuthorization()))
			throw new AccessControlException("Authorization failure");

		// Does the actual fingerprint match the expected one (if any)?
		Fingerprint.Builder fingerprintBuilder =
			Fingerprint.newBuilder()
				.setContent(block.getBytes());

		if (!block.getFingerprintAlgorithm().isEmpty())
			fingerprintBuilder.setAlgorithm(block.getFingerprintAlgorithm());

		final String actualName = fingerprintBuilder.build().encode();
		String expected = block.getExpectedName();
		if (!expected.isEmpty() && !expected.equals(actualName))
			throw new IllegalArgumentException(
					"Block name (" + actualName
					 + ") does not match expected name (" + block.getExpectedName()
					 + ")");

		final ByteBuffer bytes = block.getBytes();

		final ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(bytes.remaining());

		InputStream stream = new ByteArrayInputStream(bytes.array());

		try
		{
			s3.putObject(USER_DATA_BUCKET, actualName, stream, metadata);
			s3.setObjectAcl(USER_DATA_BUCKET, actualName, DEFAULT_ACL);
		}
		catch (AmazonClientException e) { throw new RuntimeException(e); }

		return actualName;
	}


	/** In the future, this will be much more sophisticated! */
	private boolean checkAuth(String authenticator)
	{
		return (authenticator.equals(authSecret));
	}



	/** User data is public by default (but encrypted!). */
	private static final CannedAccessControlList DEFAULT_ACL = CannedAccessControlList.PublicRead;

	/** The S3 bucket to store user data in. */
	private static final String USER_DATA_BUCKET = "me.footlights.userdata";

	/** Class-specific logger. */
	private static final Logger log = Logger.getLogger(AmazonUploader.class.getCanonicalName());

	/** Amazon S3 client. */
	private final AmazonS3Client s3;

	/** Secret used to authenticate uploads. */
	private final String authSecret;
}

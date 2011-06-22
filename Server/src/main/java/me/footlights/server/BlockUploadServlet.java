package me.footlights.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.footlights.core.Preferences;
import me.footlights.core.ConfigurationError;
import me.footlights.core.crypto.Fingerprint;

import org.apache.tomcat.util.http.fileupload.DefaultFileItemFactory;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.apache.tomcat.util.http.fileupload.FileUploadException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;


/**
 * Servlet to manage the uploading of user data blocks.
 */
public class BlockUploadServlet extends HttpServlet
{
	public BlockUploadServlet(Preferences preferences)
	{
		this.config = preferences;

		final String keyId = config.getString("amazon.keyId");
		final String secret = config.getString("amazon.secretKey");

		if (keyId.isEmpty()) throw new ConfigurationError("Amazon key ID not set");
		if (secret.isEmpty()) throw new ConfigurationError("Amazon secret key not set");

		AWSCredentials cred = new AWSCredentials()
		{
			@Override public String getAWSAccessKeyId() { return keyId; }
			@Override public String getAWSSecretKey() { return secret; }
		};

		s3 = new AmazonS3Client(cred);
		uploadArena = new FileUpload(new DefaultFileItemFactory());
	}

	@Override public void init()
	{
		authSecret = config.getString("blockstore.secret");
	}


	/**
	 * Accept a file for upload, unless there's a reason not to:
	 *  - invalid request
	 *  - not authorized
	 *  - incorrect size
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		log.entering(BlockUploadServlet.class.getName(), "doPost", new Object[] { request, response });

		Level[] levels = { Level.SEVERE, Level.WARNING, Level.INFO, Level.FINE, Level.FINER, Level.FINEST };
		for (Level l : levels)
			log.log(l, "BlockUploader.doPost(): level " + l);

		final UploadedFile file;
		try
		{
			file = getFile(request);

			if (!checkAuth(file.auth))
			{
				log.info("checkAuth() failed on request from " + request.getRemoteAddr());
				response.sendError(SC_FORBIDDEN);
				return;
			}
		}
		catch (FileUploadException e)
		{
			log.log(Level.WARNING, "Upload failed", e);
			response.sendError(SC_INTERNAL_SERVER_ERROR, "Upload failed.");
			return;
		}
		catch (NoSuchAlgorithmException e)
		{
			log.log(Level.INFO, request.getRemoteAddr() + ": invalid naming algorithm", e);
			response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,
					"Invalid naming algorithm: " + e.getMessage());
			return;
		}
		catch (IllegalArgumentException e)
		{
			log.log(Level.SEVERE, request.getRemoteAddr() + ": invalid argument", e);
			response.sendError(SC_BAD_REQUEST, e.getMessage());
			return;
		}


		InputStream stream = new ByteArrayInputStream(file.bytes);

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.bytes.length);

		try
		{
			s3.putObject(USER_DATA_BUCKET, file.name, stream, metadata);
			s3.setObjectAcl(USER_DATA_BUCKET, file.name, DEFAULT_ACL);
		}
		catch (AmazonClientException e)
		{
			response.sendError(SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}

		response.setContentType("text/plain");
		response.setStatus(SC_OK);
		response.getWriter().write(file.name);
	}


	/** In the future, this will be much more sophisticated! */
	private boolean checkAuth(String authenticator)
	{
		return (authenticator.equals(authSecret));
	}


	/** Translate the HTTP request into something that we understand. */
	private UploadedFile getFile(HttpServletRequest request)
		throws FileUploadException, NoSuchAlgorithmException,
			IllegalArgumentException
	{
		UploadedFile file = new UploadedFile();

		// We only accept enctype="multipart/form-data".
		if (!FileUpload.isMultipartContent(request))
			throw new IllegalArgumentException("File not attached");

		// Parse the individual form fields. 
		@SuppressWarnings("unchecked")
		List<FileItem> items = uploadArena.parseRequest(request);
		for (FileItem i: items)
		{
			switch (FormFields.valueOf(i.getFieldName()))
			{
				case AUTHENTICATOR:    file.auth = i.getString();         break;
				case DIGEST_ALGORITHM: file.algorithm = i.getString();    break;
				case EXPECTED_NAME:    file.name = i.getString();         break;
				case FILE_CONTENTS:    file.bytes = i.get();              break;

				default:
					throw new IllegalArgumentException(
							"Unknown field '" + i.getFieldName() + "'");
			}
		}

		file.checkName();
		return file;
	}


	/** Data that the user has uploaded. */
	private class UploadedFile
	{
		private byte[] bytes = null;
		private String auth = "";
		private String algorithm = "";
		private String name = "";

		void checkName()
			throws IllegalArgumentException, NoSuchAlgorithmException
		{
			if (bytes == null)
				throw new IllegalArgumentException("No file attached");

			Fingerprint.Builder fingerprintBuilder =
				Fingerprint.newBuilder()
					.setContent(bytes);

			if (!algorithm.isEmpty()) fingerprintBuilder.setAlgorithm(algorithm);

			String actualName = fingerprintBuilder.build().encode();

			if (name.isEmpty()) name = actualName;
			else if (!name.equals(actualName))
				throw new IllegalArgumentException(
						"Block name (" + actualName
						 + ") does not match expected name (" + name
						 + ")");
		}
	}

	/** Fields that we expect the submitter to provide. */
	private enum FormFields
	{
		AUTHENTICATOR,
		DIGEST_ALGORITHM,
		EXPECTED_NAME,
		FILE_CONTENTS,
	}


	private static final Logger log = Logger.getLogger(BlockUploadServlet.class.getCanonicalName());

	/** User data is public by default (but encrypted!). */
	private static final CannedAccessControlList DEFAULT_ACL =
		CannedAccessControlList.PublicRead;

	/** The S3 bucket to store user data in. */
	private static final String USER_DATA_BUCKET = "me.footlights.userdata";

	/** Temporary storage for uploaded files. */
	private final FileUpload uploadArena;

	/** Amazon S3 client. */
	private final AmazonS3Client s3;

	private String authSecret;

	/** Footlights configuration. */
	private final Preferences config;


	private static final long serialVersionUID =
		("28 Apr 2011 1603h" + BlockUploadServlet.class.getCanonicalName())
		.hashCode();
}

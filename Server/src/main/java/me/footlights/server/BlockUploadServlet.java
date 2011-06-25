package me.footlights.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.DefaultFileItemFactory;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.apache.tomcat.util.http.fileupload.FileUploadException;

import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;


/**
 * Servlet to manage the uploading of user data blocks.
 */
public class BlockUploadServlet extends HttpServlet
{
	public BlockUploadServlet()
	{
		uploadArena = new FileUpload(new DefaultFileItemFactory());

		Injector injector = Guice.createInjector(new WebAppGuiceModule());
		uploader = injector.getInstance(Uploader.class);
	}


	/**
	 * Accept a file for upload, unless there's a reason not to:
	 *  - invalid request
	 *  - not authorized
	 *  - incorrect size
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		log.entering(BlockUploadServlet.class.getName(), "doPost", new Object[] { request, response });

		// We only accept enctype="multipart/form-data".
		if (!FileUpload.isMultipartContent(request))
		{
			response.sendError(SC_BAD_REQUEST, "File not attached");
			return;
		}


		// Parse the form.
		final Uploader.Block toUpload;
		try { toUpload = parseForm(request); }
		catch (FileUploadException e)
		{
			log.log(Level.INFO, "Upload failed", e);
			response.sendError(SC_INTERNAL_SERVER_ERROR, "Upload failed.");
			return;
		}
		catch (Throwable t)
		{
			log.log(Level.SEVERE, "Uncaught exception in BlockUploadServlet.parseForm()", t);
			response.sendError(SC_INTERNAL_SERVER_ERROR, t.getMessage());
			return;
		}


		// Upload the block.
		try
		{
			final String name = uploader.upload(toUpload);

			response.setContentType("text/plain");
			response.setStatus(SC_OK);
			response.getWriter().write(name);
		}
		catch (AccessControlException e)
		{
			log.info(
				"checkAuth() failed on request from " + request.getRemoteAddr()
				 + "; auth = '" + toUpload.getAuthorization() + "'");
			response.sendError(SC_FORBIDDEN);
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
			log.log(Level.INFO, request.getRemoteAddr() + ": invalid argument", e);
			response.sendError(SC_BAD_REQUEST, e.getMessage());
			return;
		}
		catch (Throwable t)
		{
			log.log(Level.SEVERE, "Uncaught exception in uploader.upload()", t);
			response.sendError(SC_INTERNAL_SERVER_ERROR, t.getMessage());
			return;
		}
	}


	/** Parse a multipart/form-data POST request. */
	private Uploader.Block parseForm(HttpServletRequest request) throws FileUploadException
	{
		@SuppressWarnings("unchecked")
		List<FileItem> items = uploadArena.parseRequest(request);

		final Map<FormFields,byte[]> params = Maps.newHashMap();
		for (FileItem i: items)
			params.put(FormFields.valueOf(i.getFieldName()), i.get());

		byte[] rawBytes = params.get(FormFields.FILE_CONTENTS);
		if (rawBytes == null) throw new FileUploadException("No file attached");

		final ByteBuffer bytes = ByteBuffer.wrap(rawBytes);
		final String auth = new String(params.get(FormFields.AUTHENTICATOR));
		final String fingerprint = new String(params.get(FormFields.DIGEST_ALGORITHM));
		final String name = new String(params.get(FormFields.EXPECTED_NAME));

		return new Uploader.Block()
			{
				@Override public ByteBuffer getBytes() { return bytes; }
				@Override public String getAuthorization() { return auth; }
				@Override public String getFingerprintAlgorithm() { return fingerprint; }
				@Override public String getExpectedName() { return name; }
			};
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

	/** Temporary storage for uploaded files. */
	private final FileUpload uploadArena;

	/** The client that actually uploads blocks to a backend service. */
	private final Uploader uploader;


	private static final long serialVersionUID =
		("22 Jun 2011 0942h" + BlockUploadServlet.class.getCanonicalName())
		.hashCode();
}

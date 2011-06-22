package me.footlights.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.security.NoSuchAlgorithmException;


/** Something which can upload blocks to a remote server. */
interface Uploader
{
	/** A block of data that the user has uploaded. */
	interface Block
	{
		/** Read-only view of the uploaded bytes. Guaranteed not to be null. */
		ByteBuffer getBytes();

		String getAuthorization();
		String getFingerprintAlgorithm();
		String getExpectedName();
	}

	String upload(Block file)
		throws AccessControlException, IOException, NoSuchAlgorithmException, RuntimeException;
}

package me.footlights;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;


/**
 * A logical file.
 *
 * Files are immutable; to modify a file, you must call {@link mutable()}, which returns a
 * {@link MutableFile}, modify that, and {@link MutableFile.freeze()} it.
 */
public interface File
{
	public interface MutableFile
	{
		/** TODO: break content into appropriate-sized blocks. */
		public MutableFile setContent(Iterable<ByteBuffer> content);

		/**
		 * Produce a proper {@link File} by fixing the current contents of this
		 * {@link MutableFile}.
		 */
		public File freeze() throws GeneralSecurityException;
	}


	/**
	 * The content of the file, transformed into an {@link InputStream}.
	 */
	public InputStream getInputStream();
}

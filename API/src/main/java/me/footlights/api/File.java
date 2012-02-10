/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.footlights.api;

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
	 * The file's name.
	 *
	 * Client code should treat filenames as opaque identifiers; they are certainly not guaranteed
	 * to be human-readable.
	 */
	public String name();

	/**
	 * The content of the file, transformed into an {@link InputStream}.
	 */
	public InputStream getInputStream();
}

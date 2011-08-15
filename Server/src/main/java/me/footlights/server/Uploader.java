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

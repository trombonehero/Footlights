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
package me.footlights.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;



/** A plugin's interface to the Footlights core. */
public interface KernelInterface
{
	/** A silly example of a syscall. */
	public java.util.UUID generateUUID();

	/**
	 * Save data to a logical file.
	 */
	public File save(ByteBuffer data) throws IOException;

	/** Open a named file. */
	public File open(String name) throws IOException;

	/** Open a file on the local machine (e.g. a photo to upload). */
	public File openLocalFile() throws IOException;
}

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

import java.io.IOException;
import java.nio.ByteBuffer;

import scala.Option;


/** An application's interface to the Footlights core. */
public interface KernelInterface
{
	/**
	 * Save data to a logical file.
	 */
	public Option<File> save(ByteBuffer data) throws IOException;

	/** Open a named file. */
	public Option<File> open(String name) throws IOException;

	/** Open a file on the local machine (e.g. a photo to upload). */
	public Option<File> openLocalFile() throws IOException;
}

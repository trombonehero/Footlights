/*
 * Copyright 2012 Jonathan Anderson
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
import java.net.URI;
import java.nio.ByteBuffer;

import scala.Option;


/**
 * A mapping from application-specified names to {@link File} and {@link Directory} objects.
 *
 * A {@link Directory} is mutable from an application perspective, but maps onto
 * immutable structures behind the {@link KernelInterface}.
 */
public interface Directory
{
	public interface Entry
	{
		public boolean isDir();

		public Option<Directory> directory();
		public Option<File> file();
	}

	/** The name of the immutable directory which represents the current directory state. */
	public URI snapshotName();

	/** All current directory entries. */
	public Iterable<Entry> entries();

	/** Open a {@link File} by relative name. */
	public Option<File> open(String name);

	/** Retrieve an {@link Entry} by relative name. */
	public Option<Entry> get(String name);

	/** Store a {@link File} using a relative name. */
	public Entry save(String name, File file);

	/** Save some data using a relative name. */
	public Entry save(String name, ByteBuffer data);

	/** Store a nested {@link Directory} using a relative name. */
	public Entry save(String name, Directory directory);

	public Directory mkdir(String name) throws IOException;
}

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

import java.net.URI;
import java.nio.ByteBuffer;

import scala.Either;
import scala.Option;
import scala.Tuple2;


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

		public String name();
		public Either<Exception,Directory> directory();
		public Either<Exception,File> file();
	}

	/** The name of the immutable directory which represents the current directory state. */
	public URI snapshotName();

	/** All current directory entries. */
	public Iterable<Entry> entries();

	/** Files (not directories) in this directory. */
	public Iterable<Tuple2<String,File>> files();

	/** Direct sub-directories in this directory. */
	public Iterable<Tuple2<String,Directory>> subdirs();

	/** Open a {@link File} by relative name. */
	public Either<Exception,File> open(String name);

	/** Open a {@link Directory} by relative name. */
	public Either<Exception,Directory> openDirectory(String name);

	/** Retrieve an {@link Entry} by relative name. */
	public Option<Entry> get(String name);

	/** Store a {@link File} using a relative name. */
	public Either<Exception,Entry> save(String name, File file);

	/** Save some data using a relative name. */
	public Either<Exception,Entry> save(String name, ByteBuffer data);

	/** Store a nested {@link Directory} using a relative name. */
	public Either<Exception,Entry> save(String name, Directory directory);

	/**
	 * Create a subdirectory with a given name.
	 *
	 * This will fail if the given name is already used by an existing file or directory.
	 */
	public Either<Exception,Directory> mkdir(String name);

	/**
	 * Remove an entry (file or directory).
	 *
	 * @return   this directory on success (minus specified entry)
	 *           or an Exception if the specified entry does not exist
	 */
	public Either<Exception,Directory> remove(String name);
}

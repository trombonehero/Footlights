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
package me.footlights.core.data.store;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import scala.Option;

import me.footlights.core.FileBackedPreferences;
import me.footlights.core.Preferences;
import me.footlights.core.crypto.Fingerprint;
import me.footlights.core.data.FormatException;


/** A block store on disk */
public class DiskStore extends LocalStore
{
	public static class Builder
	{
		public DiskStore build() { return new DiskStore(dir, cache); }

		public Builder setCache(Option<LocalStore> cache)	{ this.cache = cache;	return this; }
		public Builder setDirectory(File dir)				{ this.dir   = dir;		return this; }
		public Builder setPreferences(Preferences prefs)	{ this.prefs = prefs;	return this; }

		public Builder setDefaultDirectory()
		{
			dir = new File(prefs.getString(FileBackedPreferences.CACHE_DIR_KEY()).get());
			dir.mkdirs();

			return this;
		}

		public Builder createTemporaryDirectory() throws IOException
		{
			dir = File.createTempFile("cache", "dir");
			dir.delete();
			dir.mkdir();

			return this;
		}

		private Builder()
		{
			cache = Option.apply((LocalStore) new MemoryStore());
		}

		private File dir;
		private Option<LocalStore> cache;
		private Preferences prefs = Preferences.getDefaultPreferences();
	}

	public static Builder newBuilder() { return new Builder(); }

	private DiskStore(File storageDirectory, Option<LocalStore> cache)
	{
		super(cache);
		this.dir = storageDirectory;
	}


	@Override
	public Collection<Stat> list() throws IOException
	{
		String names[] = dir.list();

		Collection<Stat> l = new ArrayList<Stat>(names.length);
		for (String name : names)
		{
			name = dir + File.separator + name;
			File f = new File(name);
			if (!f.exists())
			{
				log.log(Level.SEVERE, "Cached file does not exist: " + name);
				continue;
			}

			try { l.add(Stat.apply(f)); }
			catch (Exception e) { log.log(Level.WARNING, "Problem with cached file's name", e); }
		}

		return l;
	}


	@Override
	public void put(Fingerprint name, ByteBuffer buffer) throws IOException
	{
		FileChannel channel = new FileOutputStream(new File(dir, name.encode())).getChannel();
		channel.write(buffer.duplicate());
		channel.force(true);
	}


	@Override
	public Option<ByteBuffer> get(Fingerprint name)
	{
		try
		{
			File file = new File(dir, name.encode());
			long len = file.length();

			if (len <= 0) return Option.apply(null);
			else if (len > MAX_FILE_SIZE)
			{
				log.warning("Block is too large to load (" + len + "B, max loadable size is "
					 + MAX_FILE_SIZE + "B)");
				return Option.apply(null);
			}

			// The file is a valid block, smaller than MAX_FILE_SIZE (so < 2^31).
			// Read it if it's small, mmap it if it's large.
			FileChannel channel = new FileInputStream(file).getChannel();

			if (len > MAX_READ_SIZE)
				return Option.apply((ByteBuffer) channel.map(MapMode.READ_ONLY, 0, len));
			else
			{
				ByteBuffer buffer = ByteBuffer.allocate((int) len);
				channel.read(buffer);
				buffer.rewind();
				return Option.apply(buffer.asReadOnlyBuffer());
			}
		}
		catch(FileNotFoundException e) { log.log(Level.FINE, "Missing block", e); }
		catch(FormatException e) { log.log(Level.WARNING, "Mangled block", e); }
		catch(IOException e) { log.log(Level.WARNING, "Error reading file", e); }

		return Option.apply(null);
	}

	@Override public String toString() { return "DiskStore { " + dir + " }"; }


	/** The largest block file that we will open - positive 2s-complement. */
	private static int MAX_FILE_SIZE = 0x7FFFFFFF;

	/** Files larger than this value will be mmap'ed, rather than read. */
	private static int MAX_READ_SIZE = (1 << 20);

	private static Logger log = Logger.getLogger(DiskStore.class.getCanonicalName());

	/** The directory that we store files in. */
	private final File dir;
}

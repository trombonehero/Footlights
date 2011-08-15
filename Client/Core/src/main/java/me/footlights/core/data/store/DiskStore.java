package me.footlights.core.data.store;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.AbstractCollection;
import java.util.LinkedList;

import me.footlights.core.FileBackedPreferences;
import me.footlights.core.Preferences;
import me.footlights.core.data.FormatException;
import me.footlights.core.data.NoSuchBlockException;


/** A block store on disk */
public class DiskStore extends LocalStore
{
	public static class Builder
	{
		public DiskStore build() { return new DiskStore(dir, cache); }

		public Builder setCache(LocalStore cache)			{ this.cache = cache;	return this; }
		public Builder setDirectory(File dir)				{ this.dir = dir;		return this; }
		public Builder setPreferences(Preferences prefs)	{ this.prefs = prefs;	return this; }

		public Builder setDefaultDirectory()
		{
			dir = new File(prefs.getString(FileBackedPreferences.CACHE_DIR_KEY));
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
			cache = new MemoryStore();
		}

		private File dir;
		private LocalStore cache;
		private Preferences prefs = Preferences.getDefaultPreferences();
	}

	public static Builder newBuilder() { return new Builder(); }

	private DiskStore(File storageDirectory, LocalStore cache)
	{
		super(cache);
		this.dir = storageDirectory;
	}


	@Override
	public AbstractCollection<String> list() throws IOException
	{
		String names[] = dir.list();
		
		AbstractCollection<String> l = new LinkedList<String>();
		for(String name : names) l.add(name);

		return l;
	}
	
	
	@Override
	protected void put(String name, ByteBuffer buffer) throws IOException
	{
		new FileOutputStream(new File(dir, name))
			.getChannel()
			.write(buffer.duplicate());
	}


	@Override
	protected ByteBuffer get(String name)
		throws IOException, NoSuchBlockException
	{
		try
		{
			File file = new File(dir, name);
			long len = file.length();
			
			if (len <= 0) throw new NoSuchBlockException(this, name);
			else if (len > MAX_FILE_SIZE)
				throw new IOException(
					"Block is too large to load (" + len + "B, max loadable size is "
					 + MAX_FILE_SIZE + "B)");

			// The file is a valid block, smaller than MAX_FILE_SIZE (so < 2^31).
			// Read it if it's small, mmap it if it's large.
			FileChannel channel = new FileInputStream(file).getChannel();

			if (len > MAX_READ_SIZE)
				return channel.map(MapMode.READ_ONLY, 0, len);
			else
			{
				ByteBuffer buffer = ByteBuffer.allocate((int) len);
				channel.read(buffer);
				buffer.rewind();
				return buffer.asReadOnlyBuffer();
			}
		}
		catch(FileNotFoundException e)
		{
			throw new NoSuchBlockException(this, name);
		}
		catch(FormatException e)
		{
			throw new IOException("Mangled block: " + e);
		}
	}


	/** The largest block file that we will open - positive 2s-complement. */
	private static int MAX_FILE_SIZE = 0x7FFFFFFF;

	/** Files larger than this value will be mmap'ed, rather than read. */
	private static int MAX_READ_SIZE = (1 << 20);

	/** The directory that we store files in. */
	private final File dir;
}

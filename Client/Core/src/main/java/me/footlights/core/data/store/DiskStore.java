package me.footlights.core.data.store;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.AbstractCollection;
import java.util.LinkedList;

import me.footlights.core.Config;
import me.footlights.core.data.FormatException;
import me.footlights.core.data.NoSuchBlockException;


/** A block store on disk */
public class DiskStore extends LocalStore
{
	public DiskStore()
	{
		this(new MemoryStore());
	}

	public DiskStore(LocalStore cache)
	{
		super(cache);

		String pathsep = System.getProperty("file.separator");
		dirname = Config.getInstance().directory() + pathsep + "cache";

		dir = new File(dirname);
		dir.mkdirs();

		dirname += pathsep;
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
		new FileOutputStream(dirname + name)
			.getChannel()
			.write(buffer.duplicate());
	}


	@Override
	protected ByteBuffer get(String name)
		throws IOException, NoSuchBlockException
	{
		try
		{
			File file = new File(dirname + name);
			long len = file.length();
			
			if (len <= 0) throw new NoSuchBlockException(this, name);
			else if (len > MAX_FILE_SIZE)
				throw new IOException(
					"Block is too large to load (" + len + "B, max loadable size is "
					 + MAX_FILE_SIZE + "B)");

			// The file is a valid block, smaller than MAX_FILE_SIZE (so < 2^31).
			// Read it if it's small, mmap it if it's large.
			FileChannel channel = new FileInputStream(dirname + name).getChannel();

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
	private File dir;

	/** {@link #dir}'s filename. */
	private String dirname;
}

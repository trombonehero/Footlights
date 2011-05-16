package me.footlights.core.data.store;

import java.nio.ByteBuffer;
import java.util.*;

import me.footlights.core.data.NoSuchBlockException;



/** A block store in memory. */
public class MemoryStore extends LocalStore
{
	public MemoryStore()
	{
		super(null);
		blocks = new HashMap<String,ByteBuffer>();
	}

	
	@Override
	public AbstractCollection<String> list()
	{
		return (AbstractSet<String>) blocks.keySet();
	}

	@Override
	public void put(String name, ByteBuffer bytes) 
	{
		if (name == null) throw new NullPointerException();
		blocks.put(name, bytes);
	}

	@Override public ByteBuffer get(String name) throws NoSuchBlockException
	{
		ByteBuffer buffer = blocks.get(name);

		if (buffer == null) throw new NoSuchBlockException(this, name);
		else return buffer.asReadOnlyBuffer();
	}

	@Override
	public void flush() { /* do nothing; this class always blocks */ }


	/** That actual block store */
	private Map<String,ByteBuffer> blocks;
}

package me.footlights.core.data.store;

import java.nio.ByteBuffer;
import java.util.*;



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
		blocks.put(name, bytes);
	}

	@Override
	public ByteBuffer get(String name)
	{
		return blocks.get(name).asReadOnlyBuffer();
	}

	@Override
	public void flush() { /* do nothing; this class always blocks */ }


	/** That actual block store */
	private Map<String,ByteBuffer> blocks;
}

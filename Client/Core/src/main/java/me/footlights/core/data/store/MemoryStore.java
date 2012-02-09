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

import java.nio.ByteBuffer;
import java.util.*;

import com.google.common.collect.Maps;

import me.footlights.core.crypto.Fingerprint;
import me.footlights.core.data.NoSuchBlockException;



/** A block store in memory. */
public class MemoryStore extends LocalStore
{
	public MemoryStore()
	{
		super(null);
		blocks = Maps.newHashMap();
	}

	
	@Override
	public AbstractCollection<Fingerprint> list()
	{
		return (AbstractSet<Fingerprint>) blocks.keySet();
	}

	@Override
	public void put(Fingerprint name, ByteBuffer bytes)
	{
		if (name == null) throw new NullPointerException();
		blocks.put(name, bytes);
	}

	@Override public ByteBuffer get(Fingerprint name) throws NoSuchBlockException
	{
		ByteBuffer buffer = blocks.get(name);

		if (buffer == null) throw new NoSuchBlockException(this, name);
		else return buffer.asReadOnlyBuffer();
	}

	@Override
	public void flush() { /* do nothing; this class always blocks */ }


	/** That actual block store */
	private Map<Fingerprint,ByteBuffer> blocks;
}

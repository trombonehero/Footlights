package me.footlights.core.data.store;

import java.io.IOException;
import java.util.AbstractCollection;


/**
 * A store which is in some sense "local" (e.g. in memory, on disk), so the
 * contents of the directory are available to be listed.
 */
public abstract class LocalStore extends Store
{
	public LocalStore(Store cache)
	{
		super(cache);
	}

	/**
	 * List the blocks that are stored here.
	 */
	public abstract AbstractCollection<String> list() throws IOException;
}

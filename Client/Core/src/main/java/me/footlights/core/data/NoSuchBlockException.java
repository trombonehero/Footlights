package me.footlights.core.data;

import me.footlights.core.data.store.Store;


public class NoSuchBlockException extends Exception
{
	public NoSuchBlockException(Store store, String name)
	{
		super("The store " + store + " does not contain the block " + name);
	}

	private static final long serialVersionUID =
		"footlights.core.data.NoSuchBlockException 2010-04-13 2019h".hashCode();
}

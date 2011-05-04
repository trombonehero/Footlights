package me.footlights.core.crypto;


/** A key was not found in a {@link Keychain}. */
public class KeyNotFoundException extends SecurityException
{
	public KeyNotFoundException(String name)
	{
		super("No key named '" + name + "' in keychain");
	}


	private static final long serialVersionUID =
		("3 May 2011 1821h" + KeyNotFoundException.class.getCanonicalName())
		.hashCode();
}

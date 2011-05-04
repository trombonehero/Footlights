package me.footlights.core;


/**
 * Utility class for expressing preconditions.
 * @author Jonathan Anderson (jon@footlights.me)
 */
public class Preconditions
{
	/**
	 * Checks that the supplied references are non-null.
	 * @throws IllegalArgumentException if any supplied reference is null
	 */
	public static void notNull(Object... o) throws IllegalArgumentException
	{
		for (int i = 0; i < o.length; i++)
			if (o[i] == null)
				throw new IllegalArgumentException(
					"Preconditions failed: argument " + i + " is null");
	}

	/** Non-instantiable utility class. */
	private Preconditions() {}
}

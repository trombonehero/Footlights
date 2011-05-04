package me.footlights.core.data;

import java.nio.ByteBuffer;


/**
 * An object which can be stored as bytes in a {@link Store}.
 * @author Jonathan Anderson (jon@footlights.me)
 */
public interface FootlightsPrimitive
{
	/** Returns the primitive representation of the object (likely read-only). */
	public ByteBuffer getBytes();
}

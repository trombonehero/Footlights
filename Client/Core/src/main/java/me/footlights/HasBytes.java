package me.footlights;


/** An object which can be turned into bytes to go out "on the wire". */
public interface HasBytes
{
	/** The bytes that go out on the wire. */
	public java.nio.ByteBuffer getBytes();
}

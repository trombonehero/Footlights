package me.footlights.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.footlights.File;


/** A plugin's interface to the Footlights core. */
public interface KernelInterface
{
	/** A silly example of a syscall. */
	public java.util.UUID generateUUID();

	/**
	 * Save data to a logical file.
	 */
	public File save(ByteBuffer data) throws IOException;
}

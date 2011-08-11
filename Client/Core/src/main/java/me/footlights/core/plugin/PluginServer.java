package me.footlights.core.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.footlights.File;
import me.footlights.core.Footlights;
import me.footlights.plugin.KernelInterface;


/** Services requests from plugins */
public class PluginServer implements KernelInterface
{
	public PluginServer(Footlights kernel)
	{
		this.kernel = kernel;
	}


	// KernelInterface implementation
	public java.util.UUID generateUUID()
	{
		// no security, this is just a silly demo of syscall functionality
		return kernel.generateUUID();
	}

	public File save(ByteBuffer data) throws IOException
	{
		return kernel.save(data);
	}


	/** The system core */
	private Footlights kernel;
}

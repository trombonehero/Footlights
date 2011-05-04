package me.footlights.core.plugin;


import me.footlights.core.Core;


/** Services requests from plugins */
public class PluginServer implements KernelInterface
{
	public PluginServer(Core kernel)
	{
		this.kernel = kernel;
	}


	// KernelInterface implementation
	public java.util.UUID generateUUID()
	{
		// no security, this is just a silly demo of syscall functionality
		return kernel.generateUUID();
	}


	/** The system core */
	private Core kernel;
}

package me.footlights.core.plugin;

import java.net.URI;
import java.util.logging.Logger;

import me.footlights.plugin.KernelInterface;
import me.footlights.plugin.Plugin;



/** Wrapper for plugins; ensures consistent exception handling */
public final class PluginWrapper extends Thread
{
	/** Constructor */
	public PluginWrapper(String name, URI url, Plugin plugin, KernelInterface kernel, Logger log)
	{
		super(name);

		this.name = name;
		this.url = url;
		this.plugin = plugin;
		this.kernel = kernel;
		this.log = log;
	}


	public final String getPluginName() { return name; }
	URI getOrigin() { return url; }
	Plugin getWrappedPlugin() { return plugin; }


	/** Run the plugin, trapping exceptions if necessary */
	@Override public void run() throws PluginException
	{
		try { plugin.run(kernel, log); }
		catch(Throwable t)
		{
			throw new PluginException("Error running '" + name + "' (" + url + ")", t);
		}
	}


	/** The actual plugin. */
	private final Plugin plugin;

	/** The human-readable name that we know the plugin by. */
	private final String name;

	/** Where the plugin came from. */
	private final URI url;

	/** The plugin's interface to the Footlights core. */
	private final KernelInterface kernel;

	/** Plugin-specific log. */
	private final Logger log;
}

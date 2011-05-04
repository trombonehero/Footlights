package me.footlights.core.plugin;


/** Interface for classes that care about plugin events */
public interface PluginWatcher
{
	/** A plugin has been loaded */
	public void pluginLoaded(PluginWrapper plugin);

	/**
	 * A plugin has been unloaded.
	 * 
	 * Note that this method does not include a reference to the unloaded
	 * plugin, as including such a reference would mean that its classes have
	 * not actually been unloaded.
	 */
	public void pluginUnloading(PluginWrapper plugin);
}

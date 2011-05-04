package me.footlights.core;


import me.footlights.core.plugin.*;


/** A User Interface */
public abstract class UI extends Thread implements PluginWatcher
{
	/**
	 * Default constructor.
	 * @param   name          user-readable name (e.g. "Web UI")
	 * @param   footlights    reference to the core
	 */
	public UI(String name, Footlights footlights)
	{
		super("Footlights UI: '" + name + "'");

		this.footlights = footlights;
		footlights.registerUI(this);
	}


	public void pluginLoaded(PluginWrapper plugin) {}
	public void pluginUnloading(PluginWrapper plugin) {}


	/** Reference to the core */
	protected Footlights footlights;
}

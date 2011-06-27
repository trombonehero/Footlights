package me.footlights.core;

import java.net.URI;
import java.util.*;

import me.footlights.core.plugin.*;



/** Interface to the software core */
public interface Footlights
{
	public void registerUI(UI ui);
	public void deregisterUI(UI ui);


	public Collection<PluginWrapper> plugins();
	public PluginWrapper loadPlugin(String name, URI uri) throws PluginLoadException;
	public void unloadPlugin(PluginWrapper plugin);
}

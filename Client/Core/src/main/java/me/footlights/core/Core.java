package me.footlights.core;

import java.io.FileInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.footlights.core.crypto.Keychain;
import me.footlights.core.plugin.PluginLoadException;
import me.footlights.core.plugin.PluginLoader;
import me.footlights.core.plugin.PluginWrapper;


public class Core implements Footlights
{
	public Core()
	{
		String configDirName = "~/.footlights/";

		java.io.File configDir = new java.io.File(configDirName);
		configDir.mkdir();

		keychain = new Keychain();
		final String keychainFileName = configDirName + "/keychain";

		try
		{
			keychain.importKeystoreFile(
				new FileInputStream(
					new java.io.File(keychainFileName)));
		}
		catch (Exception e)
		{
			Logger.getLogger(Core.class.getName())
				.warning("Unable to open keychain: " + e.getLocalizedMessage());
		}

		plugins          = Maps.newHashMap();
		uis              = Lists.newArrayList();
	}


	public void registerUI(UI ui) { uis.add(ui); }
	public void deregisterUI(UI ui) { uis.remove(ui); }

	public Collection<PluginWrapper> plugins() { return plugins.values(); }


	/** Load a plugin and wrap it up in a convenient wrapper */
	public PluginWrapper loadPlugin(String name, URI uri) throws PluginLoadException
	{
		if(plugins.containsKey(uri)) return plugins.get(uri);

		Logger log = Logger.getLogger(uri.toString());
		PluginWrapper plugin = new PluginLoader().loadPlugin(name, uri, log);

		plugins.put(uri, plugin);
		for(UI ui : uis) ui.pluginLoaded(plugin);

		return plugin;
	}


	/** Unload a plugin; after calling this, set ALL references to null */
	public void unloadPlugin(PluginWrapper plugin)
	{
		URI key = null;
		for(Entry<URI,PluginWrapper> e : plugins.entrySet())
			if(e.getValue().equals(plugin))
			{
				key = e.getKey();
				break;
			}

		if(key == null) return;

		for(UI ui : uis) ui.pluginUnloading(plugins.get(key));
		plugins.remove(key);
	}


	// KernelInterface implementation
	public java.util.UUID generateUUID() { return java.util.UUID.randomUUID(); }


	/** Name of the Core {@link Logger}. */
	public static final String CORE_LOG_NAME = "me.footlights.core";

	/** Our keychain */
	private Keychain keychain;

	/** Loaded plugins */
	private Map<URI,PluginWrapper> plugins;

	/** UIs which might like to be informed of events */
	private List<UI> uis;
}

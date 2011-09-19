/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.footlights.core.plugin;

import java.net.URI;

import java.util.Map;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import me.footlights.plugin.Plugin;



public class PluginLoader
{
	public PluginLoader(ClassLoader pluginClassLoader)
	{
		classLoader      = pluginClassLoader;
		plugins          = Maps.newHashMap();
	}


	public PluginWrapper loadPlugin(String name, URI uri, Logger log) throws PluginLoadException
	{
		try
		{
			if (plugins.containsKey(uri)) return plugins.get(uri);
			PluginLoader.log.info("Loading plugin '" + name + "' from " + uri);

			Class<?> c = classLoader.loadClass(uri.toString());
			Plugin plugin = (Plugin) c.newInstance();

			return new PluginWrapper(name, uri, plugin, log);
		}
		catch(Exception e) { throw new PluginLoadException(uri, e); }
	}


	/** Loads core classes */
	private final ClassLoader classLoader;

	/** Plugins we've already loaded */
	private final Map<URI,PluginWrapper> plugins;

	private static final Logger log = Logger.getLogger(PluginLoader.class.getCanonicalName());
}

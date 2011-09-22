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

import me.footlights.plugin.Plugin;



/** Wrapper for plugins; ensures consistent exception handling */
public final class PluginWrapper
{
	/** Constructor */
	public PluginWrapper(String name, URI url, Plugin plugin)
		throws PluginException
	{
		this.name = name;
		this.url = url;
		this.plugin = plugin;
	}


	public final String getPluginName() { return name; }
	URI getOrigin() { return url; }
	public Plugin getWrappedPlugin() { return plugin; }


	/** The actual plugin. */
	private final Plugin plugin;

	/** The human-readable name that we know the plugin by. */
	private final String name;

	/** Where the plugin came from. */
	private final URI url;
}

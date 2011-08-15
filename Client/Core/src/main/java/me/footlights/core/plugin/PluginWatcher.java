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

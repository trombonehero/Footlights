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
package me.footlights.core;

import java.util.Map;
import java.util.NoSuchElementException;


/**
 * Classes that provide [persistent] preferences should subclass this.
 *
 * @author Jonathan Anderson <jon@footlights.me>
 */
public abstract class PreferenceStorageEngine implements me.footlights.plugin.Preferences
{
	protected abstract Map<String,?> getAll();

	/** Subclasses must implement preferences as raw String objects. */
	protected abstract String getRaw(String key) throws NoSuchElementException;

	// Subclasses should override these methods if the platform provides type-safe preferences.
	@Override public String getString(String key) throws NoSuchElementException
	{
		return getRaw(key);
	}

	@Override public boolean getBoolean(String key) throws NoSuchElementException
	{
		return Boolean.parseBoolean(getRaw(key));
	}

	@Override public int getInt(String key) throws NoSuchElementException
	{
		return Integer.parseInt(getRaw(key));
	}

	@Override public float getFloat(String key) throws NoSuchElementException
	{
		return Float.parseFloat(getRaw(key));
	}


	static PreferenceStorageEngine wrap(final Map<String,?> map)
	{
		return new PreferenceStorageEngine()
			{
				@Override protected Map<String,?> getAll() { return map; }

				@Override
				protected String getRaw(String key) throws NoSuchElementException
				{
					if (!map.containsKey(key))
						throw new NoSuchElementException(key);

					return map.get(key).toString();
				}
			};
	}
}

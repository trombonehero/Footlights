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
package me.footlights.android;

import java.util.Map;
import java.util.NoSuchElementException;

import scala.Option;

import android.content.SharedPreferences;

import me.footlights.core.PreferenceStorageEngine;


/** Wraps up SharedPreferences to back me.footlights.core.Preferences. */
public class PreferenceAdapter extends PreferenceStorageEngine
{
	public static PreferenceAdapter wrap(SharedPreferences prefs)
	{
		return new PreferenceAdapter(prefs);
	}

	@Override public Map<String,?> getAll() { return prefs.getAll(); }

	@Override public Option<String> getRaw(String key) throws NoSuchElementException
	{
		if (!prefs.contains(key)) throw new NoSuchElementException(key);
		return Option.apply(getAll().get(key).toString());
	}

	@Override public Option<String> getString(String key) throws NoSuchElementException
	{
		if (!prefs.contains(key)) throw new NoSuchElementException(key);
		return Option.apply(prefs.getString(key, null));
	}

	@Override public Option<Boolean> getBoolean(String key) throws NoSuchElementException
	{
		if (!prefs.contains(key)) throw new NoSuchElementException(key);
		return Option.apply(prefs.getBoolean(key, false));
	}

	@Override public Option<Integer> getInt(String key) throws NoSuchElementException
	{
		if (!prefs.contains(key)) throw new NoSuchElementException(key);
		return Option.apply(prefs.getInt(key, 0));
	}

	@Override public Option<Float> getFloat(String key) throws NoSuchElementException
	{
		if (!prefs.contains(key)) throw new NoSuchElementException(key);
		return Option.apply(prefs.getFloat(key, 0));
	}


	private PreferenceAdapter(SharedPreferences prefs) { this.prefs = prefs; }

	/** The Android-specific preferences provider. */
	private final SharedPreferences prefs;
}

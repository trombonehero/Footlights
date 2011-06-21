package me.footlights.android;

import java.util.Map;
import java.util.NoSuchElementException;

import android.content.SharedPreferences;

import me.footlights.core.PreferenceStorageEngine;


/** Wraps up SharedPreferences to back me.footlights.core.Preferences. */
public class PreferenceAdapter extends PreferenceStorageEngine
{
	public static PreferenceAdapter wrap(SharedPreferences prefs)
	{
		return new PreferenceAdapter(prefs);
	}

	@Override protected Map<String,?> getAll() { return prefs.getAll(); }

	@Override protected String getRaw(String key) throws NoSuchElementException
	{
		if (!prefs.contains(key)) throw new NoSuchElementException(key);
		return getAll().get(key).toString();
	}

	@Override public String getString(String key) throws NoSuchElementException
	{
		if (!prefs.contains(key)) throw new NoSuchElementException(key);
		return prefs.getString(key, null);
	}

	@Override public boolean getBoolean(String key) throws NoSuchElementException
	{
		if (!prefs.contains(key)) throw new NoSuchElementException(key);
		return prefs.getBoolean(key, false);
	}

	@Override public int getInt(String key) throws NoSuchElementException
	{
		if (!prefs.contains(key)) throw new NoSuchElementException(key);
		return prefs.getInt(key, 0);
	}

	@Override public float getFloat(String key) throws NoSuchElementException
	{
		if (!prefs.contains(key)) throw new NoSuchElementException(key);
		return prefs.getFloat(key, 0);
	}


	private PreferenceAdapter(SharedPreferences prefs) { this.prefs = prefs; }

	/** The Android-specific preferences provider. */
	private final SharedPreferences prefs;
}

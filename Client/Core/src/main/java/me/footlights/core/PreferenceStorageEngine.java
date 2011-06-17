package me.footlights.core;

import java.util.NoSuchElementException;


/**
 * Classes that provide [persistent] preferences should subclass this.
 *
 * @author Jonathan Anderson <jon@footlights.me>
 */
abstract class PreferenceStorageEngine
{
	/** Subclasses must implement preferences as raw String objects. */
	protected abstract String getRaw(String key) throws NoSuchElementException;

	// Subclasses should override these methods if the platform provides type-safe preferences.
	public String getString(String key) throws NoSuchElementException
	{
		return getRaw(key);
	}

	public boolean getBoolean(String key) throws NoSuchElementException
	{
		return Boolean.parseBoolean(getRaw(key));
	}

	public int getInt(String key) throws NoSuchElementException
	{
		return Integer.parseInt(getRaw(key));
	}

	public float getFloat(String key) throws NoSuchElementException
	{
		return Float.parseFloat(getRaw(key));
	}

	public double getDouble(String key)	throws NoSuchElementException
	{
		return Double.parseDouble(getRaw(key));
	}
}

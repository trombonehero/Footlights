package me.footlights.plugin;


public interface ModifiablePreferences extends Preferences
{
	public Preferences set(String key, String value);
	public Preferences set(String key, boolean value);
	public Preferences set(String key, int value);
	public Preferences set(String key, float value);
}

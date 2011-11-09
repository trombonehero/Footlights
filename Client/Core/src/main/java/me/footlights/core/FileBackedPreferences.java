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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import me.footlights.plugin.ModifiablePreferences;
import me.footlights.plugin.Preferences;

import com.google.common.collect.Maps;


/**
 * Preferences backed by a Java Properties file, suitable for most platforms.
 *
 * Some platforms (e.g. Android) may prefer platform-specific mechanisms.
 */
public final class FileBackedPreferences
	extends PreferenceStorageEngine
	implements Flushable, ModifiablePreferences
{
	/** The key used to store the cache directory in Preferences. */
	public static final String CACHE_DIR_KEY = "footlights.cachedir";

	/** The key used to store the location of the Keychain. */
	static final String KEYCHAIN_KEY = "footlights.keychain";

	/** Path separator ('/' on UNIX, '\' on Windows). */ 
	private static final String SEP = System.getProperty("file.separator");

	/** The user's home directory. */
	private static final String HOME = System.getProperty("user.home");


	static FileBackedPreferences loadFromDefaultLocation() throws IOException
	{
		return load(openOrCreateConfigFile("Footlights"));
	}


	/** Load the preferences in a given file. */
	static FileBackedPreferences load(File file) throws IOException
	{
		Properties properties = new Properties();
		if (file.exists())
		{
			InputStream in = new FileInputStream(file); 
			properties.load(in);
			in.close();
		}
    	else file.createNewFile();

		if (!properties.containsKey(CACHE_DIR_KEY))
			properties.setProperty(CACHE_DIR_KEY, file.getParent() + SEP + "cache");

		if (!properties.containsKey(KEYCHAIN_KEY))
			properties.setProperty(KEYCHAIN_KEY, file.getParent() + SEP + "keychain");

		return new FileBackedPreferences(properties, file);
	}


	/** Create the configuration dir and file. */
	private static File openOrCreateConfigFile(String name)
	{
		// The Footlights directory is OS-specific.
		String homeDir = HOME + SEP;

		if (System.getProperty("os.name").contains("Windows"))
			homeDir += ("Application Support" + SEP + "Footlights");
		else
			homeDir += ".footlights";

		// Ensure that the config dir exists.
		File dir = new File(homeDir);
		dir.mkdirs();

		return new File(dir, name + ".properties");
	}


	@Override protected Map<String,?> getAll() { return Maps.fromProperties(properties); }

	protected String getRaw(String name) throws NoSuchElementException
	{
		String value = properties.getProperty(name);

		if (value == null) throw new NoSuchElementException(name);
		return String.copyValueOf(value.toCharArray());
	}

	// ModifiablePreferences implementation
	/** Set a configuration value */
	@Override public synchronized FileBackedPreferences set(String name, String value)
	{
		properties.setProperty(name, value);
		dirty = true;
		return this;
	}

	@Override public Preferences set(String k, boolean v) { return set(k, Boolean.toString(v)); }
	@Override public Preferences set(String k, int v) { return set(k, Integer.toString(v)); }
	@Override public Preferences set(String k, float v) { return set(k, Float.toString(v)); }

	/** Singleton constructor */
	private FileBackedPreferences(Properties properties, File file)
	{
		this.properties = properties;
		this.configFile = file;
	}


	/** Clean up */
	public void finalize() throws Throwable
	{
		flush();
		super.finalize();
	}


	/** Save the config file */
	public synchronized void flush() throws IOException
	{
		if (!dirty) return;

		OutputStream out = new FileOutputStream(configFile);
		String headerComment =
			"Configuration options, auto-saved "
			+ java.text.DateFormat.getDateInstance().format(new Date());

		properties.store(out, headerComment);

		dirty = false;
	}


	/** Name of the configuration file */
	private final File configFile;

	/** Actual config options */
	private final Properties properties;

	/** Have any options changed? */
	private boolean dirty;
}

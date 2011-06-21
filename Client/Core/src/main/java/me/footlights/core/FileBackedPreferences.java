package me.footlights.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import com.google.common.collect.Maps;


/**
 * Preferences backed by a Java Properties file, suitable for most platforms.
 *
 * Some platforms (e.g. Android) may prefer platform-specific mechanisms.
 */
public final class FileBackedPreferences extends PreferenceStorageEngine
{
	/** The key used to store the cache directory in Preferences. */
	public static final String CACHE_DIR_KEY = "footlights.cachedir";

	/** Path separator ('/' on UNIX, '\' on Windows). */ 
	private static final String SEP = System.getProperty("file.separator");

	/** The user's home directory. */
	private static final String HOME = System.getProperty("user.home");

	/** The configuration directory is OS-specific, but always relative to $HOME. */
	private static final String FOOTLIGHTS_DIRECTORY =
		HOME + SEP
			+ (System.getProperty("os.name").equals("Windows")
				? ("Application Support" + SEP + "Footlights")
				: (".footlights")
			);


	static FileBackedPreferences loadFromDefaultLocation() throws IOException
	{
		return load(openOrCreateConfigFile("Footlights"));
	}

	/** Create the configuration dir and file. */
	static File openOrCreateConfigFile(String name)
	{
		// Ensure that the config dir exists.
		File dir = new File(FOOTLIGHTS_DIRECTORY);
		dir.mkdirs();

		return new File(dir, name + ".properties");
	}


	/** Load the preferences in a given file. */
	static FileBackedPreferences load(File file) throws IOException
	{
		Properties properties = new Properties();
		properties.setProperty(CACHE_DIR_KEY, FOOTLIGHTS_DIRECTORY);

		if (file.exists())
		{
			InputStream in = new FileInputStream(file); 
			properties.load(in);
			in.close();
		}
    	else file.createNewFile();

		return new FileBackedPreferences(properties, file);
	}


	@Override protected Map<String,?> getAll() { return Maps.fromProperties(properties); }

	protected String getRaw(String name) throws NoSuchElementException
	{
		String value = properties.getProperty(name);

		if(value == null) throw new NoSuchElementException(name);
		return String.copyValueOf(value.toCharArray());
	}

	/** Set a configuration value */
	protected synchronized void set(String name, String value)
	{
		properties.setProperty(name, value);
		dirty = true;
	}


	/** Singleton constructor */
	private FileBackedPreferences(Properties properties, File file)
	{
		this.properties = properties;
		this.configFile = file;
	}


	/** Clean up */
	public void finalize() throws Throwable
	{
		saveConfig();
		super.finalize();
	}


	/** Save the config file */
	protected synchronized void saveConfig() throws IOException
	{
		if(!dirty) return;

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

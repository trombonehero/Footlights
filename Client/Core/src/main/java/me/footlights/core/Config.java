package me.footlights.core;

import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;


public class Config
{
	/** Singleton access */
	public static Config getInstance()
	{
		if(instance == null) instance = new Config();
		return instance;
	}


	/** Get a configuration value */
	public String get(String name)
	{
		String value = properties.getProperty(name);

		if(value == null) return "";
		return String.copyValueOf(value.toCharArray());
	}

	/** Set a configuration value */
	protected void set(String name, String value)
	{
		properties.setProperty(name, value);
		dirty = true;
	}


	/** Singleton constructor */
	protected Config() { loadConfig(createConfigDirectory(), "Footlights"); }


	/** Clean up */
	public void finalize() throws Throwable
	{
		saveConfig();
		super.finalize();
	}
	
	
	/** Config directory */
	public String directory()
	{
		String configDirName = System.getProperty("user.home");

		// the config directory name depends on Windows vs POSIX
		if(System.getProperty("os.name").equals("Windows"))
			configDirName += "\\Application Support\\Footlights";

		else configDirName += "/.footlights/";
		
		return configDirName;
	}


	/** Create the config dir */
	protected File createConfigDirectory()
	{
		File dir = new File(directory());
		dir.mkdir();

		return dir;
	}


	/** Set up config options */
	protected void loadConfig(File configDir, String name)
	{
		properties = new Properties(defaults());


		// load config file if one can be found
		if((configDir == null) || (name == null) || (name.length() == 0))
			return;

		for(File f : configDir.listFiles())
			if(f.getName().equals(name + ".properties"))
			{
				configFile = f;
				break;
			}

		if(configFile == null)
		{
			try
			{
				setupCrypto();

				configFile = new File(configDir.getAbsolutePath()
				                       + System.getProperty("file.separator")
				                       + name + ".properties");
				configFile.createNewFile();

				saveConfig();
			}
			catch(GeneralSecurityException e) { throw new Error(e); }
			catch(IOException e) { System.err.println(e); }

			return;
		}

		try
		{
			InputStream in = new FileInputStream(configFile); 
			properties.load(in);
			in.close();
		}
		catch(IOException e) {}
	}


	/** Save the config file */
	protected void saveConfig() throws IOException
	{
		if(!dirty) return;

		OutputStream out = new FileOutputStream(configFile);
		properties.store(out, "Configuration options, auto-saved " +
				java.text.DateFormat.getDateInstance().format(new Date()));

		dirty = false;
	}


	/** Default configuration options */
	protected Properties defaults()
	{
		Properties defaults = new Properties();

		defaults.setProperty("crypto.hash.algorithm", "SHA1");
		defaults.setProperty("crypto.sym.cipher", "AES");
		defaults.setProperty("crypto.sym.keysize", "128");
		defaults.setProperty("crypto.sym.mode", "CTR");
		defaults.setProperty("crypto.sym.padding", "NOPADDING");
		defaults.setProperty("crypto.sig.algorithm", "SHA256withRSA");
		defaults.setProperty("crypto.keystore.type", "JKS");
		defaults.setProperty("crypto.cert.validity",
		                     Integer.toString(60 * 60 * 24 * 3650));

		return defaults;
	}


	/** Set up sensible crypto options */
	protected void setupCrypto()
		throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		Cipher c = Cipher.getInstance("AES");
		Provider provider = c.getProvider();


		// symmetric-key cipher
		String symPreferences[] = { "AES", "TripleDES", "Blowfish" };
		Provider.Service cipher = null;

		for(String p : symPreferences)
			if((cipher = provider.getService("Cipher", p)) != null) break;

		if(cipher == null)
		{
			String message = "None of the desired symmetric-key ciphers ([ ";
			for(String s : symPreferences) message += s + " ";
			message += "]) are provided by " + provider;

			throw new NoSuchAlgorithmException(message);
		}
		else set("crypto.sym.algorithm", cipher.getAlgorithm());


		String mode = null;
		String m[] = cipher.getAttribute("SupportedModes").split("\\|");
		List<String> modes = Arrays.asList(m);

		String modePreferences[] = { "GCM", "CTR", "CBC" };
		for(String p : modePreferences)
			if(modes.contains(p)) { mode = p; break; }

		if(mode == null)
		{
			String message = "None of the desired symmetric-key modes ([ ";
			for(String s : modePreferences) message += s + " ";
			message += "]) are provided by " + provider + "; it provides [ ";
			for(String s : modes) message += s + " ";
			message += "]";

			throw new NoSuchAlgorithmException(message);
		}
		else set("crypto.sym.mode", mode);



		// don't use padding (we prefer stream ciphers, anyway)
		set("crypto.sym.padding", "NOPADDING");



		// asymmetric-key cipher
		String asymPreferences[] = { "RSA" /* TODO: more? */ };
		cipher = null;

		for(String p : asymPreferences)
			if((cipher = provider.getService("Cipher", p)) != null) break;

		if(cipher == null)
		{
			String message = "None of the desired asymmetric-key ciphers ([ ";
			for(String s : asymPreferences) message += s + " ";
			message += "]) are provided by " + provider;

			throw new NoSuchAlgorithmException(message);
		}
		else set("crypto.asym.algorithm", cipher.getAlgorithm());


		set("crypto.hash.algorithm", "SHA-256");
	}


	/** Singleton instance */
	private static Config instance;

	/** Name of the configuration file */
	private File configFile;

	/** Actual config options */
	private Properties properties;

	/** Have any options changed? */
	private boolean dirty;
}

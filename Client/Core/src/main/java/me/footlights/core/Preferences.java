package me.footlights.core;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import com.google.common.collect.Maps;


public class Preferences
{
	public static Preferences getDefaultPreferences() { return new Preferences(null); }

	public String getString(String key) throws NoSuchElementException
	{
		if (engine != null)
			try { return engine.getString(key); }
			catch (NoSuchElementException e) {}

		return defaults.getString(key);
	}

	public boolean getBoolean(String key) throws NoSuchElementException 
	{
		if (engine != null)
			try { return engine.getBoolean(key); }
			catch (NoSuchElementException e) {}

		return defaults.getBoolean(key);
	}

	public int getInt(String key) throws NoSuchElementException
	{
		if (engine != null)
			try { return engine.getInt(key); }
			catch (NoSuchElementException e) {}

		return defaults.getInt(key);
	}

	public float getFloat(String key) throws NoSuchElementException
	{
		if (engine != null)
			try { return engine.getFloat(key); }
			catch (NoSuchElementException e) {}

		return defaults.getFloat(key);
	}


	/** Singleton constructor */
	private Preferences(PreferenceStorageEngine prefs)
	{
		this.engine = prefs;

		try
		{
    		final Map<String,String> defaults = cryptoDefaults();
    		this.defaults = new PreferenceStorageEngine()
    		{	
    			@Override
    			protected String getRaw(String key) throws NoSuchElementException
    			{
    				if (!defaults.containsKey(key))
    					throw new NoSuchElementException(key);

    				return defaults.get(key);
    			}
    		};
		}
		catch (GeneralSecurityException e) { throw new ConfigurationError(e); }
	}



	/** Set up sensible crypto options */
	private Map<String,String> cryptoDefaults()
		throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		Map<String,String> defaults = Maps.newHashMap();


		String[][] values = 
		{
			{ "crypto.asym.keylen", "2048" },
			{ "crypto.prng", "SHA1PRNG" },
			{ "crypto.sym.keylen", "256" },
			{ "crypto.sig.algorithm", "SHA256withRSA" },
			{ "crypto.keystore.type", "JKS" },
			{ "crypto.cert.validity", Integer.toString(60 * 60 * 24 * 3650) },
		};

		for (String[] pair : values)
			defaults.put(pair[0], pair[1]);


		Cipher c = Cipher.getInstance("AES");
		Provider provider = c.getProvider();


		// symmetric-key cipher
		String symPreferences[] = { "AES", "TripleDES", "Blowfish" };
		String modePreferences[] = { "GCM", "CTR", "CBC" };

		Provider.Service cipher = null;
		List<String> modes = null;

		for(String p : symPreferences)
		{
			cipher = provider.getService("Cipher", p);
			if (cipher == null) continue;

			String rawModes = cipher.getAttribute("SupportedModes");
			if (rawModes == null)
			{
				// Not all providers tell us what modes they support (I'm looking at you, Android!).
				// We'll just have to hope for the best.
				modes = Arrays.asList(modePreferences);
			}
			else modes = Arrays.asList(rawModes.split("\\|"));
		}

		if(cipher == null)
		{
			String message = "None of the desired symmetric-key ciphers ([ ";
			for(String s : symPreferences) message += s + " ";
			message += "]) are provided by " + provider;

			throw new NoSuchAlgorithmException(message);
		}
		else defaults.put("crypto.sym.algorithm", cipher.getAlgorithm());


		String mode = null;
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
		else defaults.put("crypto.sym.mode", mode);



		// don't use padding (we prefer stream ciphers, anyway)
		defaults.put("crypto.sym.padding", "NOPADDING");



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
		else defaults.put("crypto.asym.algorithm", cipher.getAlgorithm());


		defaults.put("crypto.hash.algorithm", "SHA-256");

		return defaults;
	}


	/** Platform-specific preference storage. */
	private final PreferenceStorageEngine engine;

	/** Fallback defaults. */
	private final PreferenceStorageEngine defaults;
}

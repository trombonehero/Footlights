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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.common.collect.Maps;


public class Preferences implements me.footlights.api.Preferences, HasBytes
{
	/** Create a Preferences instance with auto-detected default settings. */
	public static Preferences getDefaultPreferences()
	{
		if (defaultPreferences != null) return defaultPreferences;
		else return new Preferences(null);
	}

	/** Load Preferences from the default filesystem location. */
	public static Preferences loadFromDefaultLocation() throws IOException
	{
		return create(FileBackedPreferences.loadFromDefaultLocation());
	}

	/** Create preferences with an explicit backing engine. */
	public static Preferences create(PreferenceStorageEngine engine)
	{
		Preferences prefs = new Preferences(engine);
		if (defaultPreferences == null) defaultPreferences = prefs;
		return prefs;
	}


	public static Preferences wrap(Map<String,?> map)
	{
		return create(PreferenceStorageEngine.wrap(map));
	}


	// Methods to actually retrieve preferences.
	public Map<String,?> getAll()
	{
		Map<String,Object> everything = Maps.newHashMap();

		everything.putAll(defaults.getAll());
		if (engine != null) everything.putAll(engine.getAll());

		return everything;
	}

	@Override public String getString(String key) throws NoSuchElementException
	{
		if (engine != null)
			try { return engine.getString(key); }
			catch (NoSuchElementException e) {}

		return defaults.getString(key);
	}

	@Override public boolean getBoolean(String key) throws NoSuchElementException
	{
		if (engine != null)
			try { return engine.getBoolean(key); }
			catch (NoSuchElementException e) {}

		return defaults.getBoolean(key);
	}

	@Override public int getInt(String key) throws NoSuchElementException
	{
		if (engine != null)
			try { return engine.getInt(key); }
			catch (NoSuchElementException e) {}

		return defaults.getInt(key);
	}

	@Override public float getFloat(String key) throws NoSuchElementException
	{
		if (engine != null)
			try { return engine.getFloat(key); }
			catch (NoSuchElementException e) {}

		return defaults.getFloat(key);
	}


	// HasBytes implementation
	@Override public ByteBuffer getBytes()
	{
		return encode(getAll());
	}

	static ByteBuffer encode(Map<String,?> snapshot)
	{
		int len = MAGIC.length + 4;  // Minimum length is magic + length representation (4B)
		for (Map.Entry<String,?> e : snapshot.entrySet())
			len += (8 + e.getKey().length() + e.getValue().toString().length());

		ByteBuffer encoded = ByteBuffer.allocate(len);
		encoded.put(MAGIC);
		encoded.putInt(snapshot.size());

		for (Map.Entry<String,?> e : snapshot.entrySet())
		{
			String key = e.getKey();
			String value = e.getValue().toString();

			encoded.putInt(key.length());
			encoded.put(key.getBytes());

			encoded.putInt(value.length());
			encoded.put(value.getBytes());
		}

		encoded.flip();
		return encoded;
	}

	/** Parse raw bytes into {@link Preferences}. */
	public static Map<String,String> parse(ByteBuffer bytes) throws IOException
	{
		byte[] magic = new byte[MAGIC.length];
		bytes.get(magic);
		if (!Arrays.equals(MAGIC, magic)) throw new IOException("Incorrect Preferences magic");

		int entries = bytes.getInt();

		Map<String,String> values = Maps.newHashMap();
		for (int i = 0; i < entries; i++)
		{
			int keylen = bytes.getInt();
			byte[] key = new byte[keylen];
			bytes.get(key);

			int valuelen = bytes.getInt();
			byte[] value = new byte[valuelen];
			bytes.get(value);

			values.put(new String(key), new String(value));
		}

		return values;
	}


	/** Singleton constructor */
	private Preferences(PreferenceStorageEngine prefs)
	{
		this.engine = prefs;

		try
		{
			// Provide some sane default for crypto settings and storage locations.
			final Map<String,String> defaultPrefs = Maps.newHashMap();
			Provider provider = Security.getProvider("BC");
			if (provider == null)
			{
				// If we haven't already installed BouncyCastle as the crypto provider, we must be
				// running in a special environment such as a unit test.
				provider = new BouncyCastleProvider();
				Security.addProvider(provider);
			}
			defaultPrefs.putAll(cryptoDefaults(provider));
			defaultPrefs.put("init.setup", "http://footlights.me/setup.json");

			// OS-specific home directory.
			String homeDir = System.getProperty("user.home") + System.getProperty("path.separator");
			if (System.getProperty("os.name").contains("Win")) homeDir += "Footlights";
			else homeDir += ".footlights";
			defaultPrefs.put("home", homeDir);

			defaults = PreferenceStorageEngine.wrap(defaultPrefs);
		}
		catch (GeneralSecurityException e) { throw new ConfigurationError(e); }
	}



	/** Set up sensible crypto options */
	private Map<String,String> cryptoDefaults(Provider provider)
		throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		Map<String,String> defaults = Maps.newHashMap();

		String[][] values = 
		{
			{ "crypto.asym.keylen", "2048" },
			{ "crypto.prng", "SHA1PRNG" },
			{ "crypto.sym.keylen", "256" },
			{ "crypto.sig.algorithm", "SHA256withRSA" },
			{ "crypto.keystore.type", "UBER" },
			{ "crypto.cert.validity", Integer.toString(60 * 60 * 24 * 3650) },
		};

		for (String[] pair : values)
			defaults.put(pair[0], pair[1]);


		// symmetric-key cipher
		String symPreferences[] = { "AES", "TripleDES", "Blowfish" };
		String modePreferences[] = { "CBC", "CTR" };

		Provider.Service cipher = null;
		List<String> modes = null;

		for (String p : symPreferences)
		{
			cipher = provider.getService("Cipher", p);
			if (cipher == null) continue;

			String rawModes = cipher.getAttribute("SupportedModes");
			if (rawModes == null)
			{
				// Not all providers tell us what modes they support (I'm looking at you, Android!).
				// We'll just have to hope for the best.
				modes = Arrays.asList(modePreferences);
				break;
			}
			else modes = Arrays.asList(rawModes.split("\\|"));
		}

		if (cipher == null)
		{
			String message = "None of the desired symmetric-key ciphers ([ ";
			for (String s : symPreferences) message += s + " ";
			message += "]) are provided by " + provider;

			throw new NoSuchAlgorithmException(message);
		}
		else defaults.put("crypto.sym.algorithm", cipher.getAlgorithm());


		String mode = null;
		for (String p : modePreferences)
			if (modes.contains(p)) { mode = p; break; }

		if (mode == null)
		{
			String message = "None of the desired symmetric-key modes ([ ";
			for (String s : modePreferences) message += s + " ";
			message += "]) are provided by " + provider + "; it provides [ ";
			for (String s : modes) message += s + " ";
			message += "]";

			throw new NoSuchAlgorithmException(message);
		}
		else defaults.put("crypto.sym.mode", mode);



		// don't use padding (we prefer stream ciphers, anyway)
		defaults.put("crypto.sym.padding", "NOPADDING");



		// asymmetric-key cipher
		String asymPreferences[] = { "RSA" /* TODO: more? */ };
		cipher = null;

		for (String p : asymPreferences)
			if ((cipher = provider.getService("Cipher", p)) != null) break;

		if (cipher == null)
		{
			String message = "None of the desired asymmetric-key ciphers ([ ";
			for (String s : asymPreferences) message += s + " ";
			message += "]) are provided by " + provider;

			throw new NoSuchAlgorithmException(message);
		}
		else defaults.put("crypto.asym.algorithm", cipher.getAlgorithm());


		defaults.put("crypto.hash.algorithm", "SHA-256");

		return defaults;
	}


	/** Magic bytes used to identify encoded {@link Preferences}: "FOOTOPTS" => "F0070475". */
	private static final byte[] MAGIC = new byte[] { (byte) 0xF0, 0x07, 0x04, 0x75 };

	/** The preferences you get if you aren't specific (the first ones created). */
	private static Preferences defaultPreferences;

	/** Platform-specific preference storage. */
	private final PreferenceStorageEngine engine;

	/** Fallback defaults. */
	private final PreferenceStorageEngine defaults;
}

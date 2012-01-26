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
import java.io.IOException
import java.nio.ByteBuffer
import java.security.{Provider,Security}
import java.security.{GeneralSecurityException,NoSuchAlgorithmException}
import java.util.{Map,NoSuchElementException}
import java.util.logging.Logger

import scala.collection.mutable.{ Map => MutableMap }
import collection.JavaConversions._

import org.bouncycastle.jce.provider.BouncyCastleProvider

import me.footlights.api.ModifiablePreferences;


package me.footlights.core {

/**
 * Classes that provide [persistent] preferences should extend this.
 *
 * @author Jonathan Anderson <jon@footlights.me>
 */
abstract class PreferenceStorageEngine extends me.footlights.api.Preferences {
	private[core] def getAll:Map[String,_]

	/** Subclasses must implement preferences as raw String objects. */
	def getRaw(key:String):Option[String]

	// Subclasses should override these methods if the platform provides type-safe preferences.
	def getString (key:String):Option[java.lang.String]  = getRaw(key)
	def getBoolean(key:String):Option[java.lang.Boolean] = getRaw(key) map { s => s.toBoolean }
	def getInt    (key:String):Option[java.lang.Integer] = getRaw(key) map { s => s.toInt }
	def getFloat  (key:String):Option[java.lang.Float]   = getRaw(key) map { s => s.toFloat }
}

object PreferenceStorageEngine {
	def wrap(map:Map[String,_]) = new PreferenceStorageEngine() {
		override def getAll = map
		override def getRaw(s:String) = if (map contains s) Option(map get(s) toString) else None
	}
}


/**
 * A mutable {@link PreferenceStorageEngine}.
 *
 * The inheriter must implement {@link #set(String,String).
 */
abstract class ModifiableStorageEngine extends PreferenceStorageEngine with ModifiablePreferences {
	override def set(key:String, value:Boolean) = { set(key, value.toString); this }
	override def set(key:String, value:Int)     = { set(key, value.toString); this }
	override def set(key:String, value:Float)   = { set(key, value.toString); this }
}


/** Serves preference information to clients. */
class Preferences(engine:Option[PreferenceStorageEngine])
	extends me.footlights.api.Preferences with HasBytes
{
	def getAll:Map[String,_] = {
		val everything = MutableMap[String,Any]()
		everything ++= defaults.getAll
		engine foreach { everything ++= _.getAll }
		everything
	}

	/** Get a value (of some type) from either the {@link #engine} or the {@link #defaults}. */ 
	def get[T](extractor:(PreferenceStorageEngine) => Option[T]) =
		engine flatMap { extractor(_) } orElse { extractor(defaults) }

	override def getString(key:String)  = get { _.getString(key) }
	override def getBoolean(key:String) = get { _.getBoolean(key) }
	override def getInt(key:String)     = get { _.getInt(key) }
	override def getFloat(key:String)   = get { _.getFloat(key) }


	// Provide some sane default for crypto settings and storage locations.
	private val defaultPrefs = MutableMap[String,String]()
	defaultPrefs ++= cryptoDefaults(security.CryptoBackend.get)
	defaultPrefs += ("init.setup" -> "http://footlights.me/setup.json")

	try {
		// OS-specific home directory.
		var homeDir = System.getProperty("user.home") + System.getProperty("path.separator")
		if (System.getProperty("os.name").contains("Win")) homeDir += "Footlights"
		else homeDir += ".footlights"

		defaultPrefs += ("home" -> homeDir)
	} catch {
		case e:GeneralSecurityException => throw new ConfigurationError(e)
	}

	private val defaults = PreferenceStorageEngine.wrap(defaultPrefs)


	/** Set up sensible crypto options */
	private def cryptoDefaults(provider:Option[Provider]):Map[String,String] = {
		val defaults = MutableMap(
			("crypto.asym.keylen"    -> "2048"),
			("crypto.hash.algorithm" -> "SHA-256"),
			("crypto.keystore.type"  -> "UBER"),
			("crypto.prng"           -> "SHA1PRNG"),
			("crypto.sym.keylen"     -> "256"),
			("crypto.sym.padding"    -> "NOPADDING"),
			("crypto.sig.algorithm"  -> "SHA256withRSA"),
			("crypto.cert.validity"  -> (60 * 60 * 24 * 3650).toString)
		)

		// Find a reasonable default symmetric cipher and mode.
		var preferred:List[String] = List("AES", "TripleDES", "Blowfish")
		val ciphers = MutableMap() ++ (preferred flatMap { cipher =>
				provider map { _.getService("Cipher", cipher) }
			} filter { _ != null } filter { c => preferred contains c.getAlgorithm } map {
				c => (c.getAlgorithm(), c)
			}
		)
		val cipher = preferred.find { ciphers.contains } flatMap { ciphers.get }
		cipher foreach { c => defaults += ("crypto.sym.algorithm" -> c.getAlgorithm()) }
 
		preferred = List("CBC", "CTR")
		val modes = cipher map { _.getAttribute("SupportedModes") } filter {
			_ != null } map { _.split("\\|") } map { List.fromArray(_)
		} filter { preferred.contains } getOrElse preferred
		preferred find { modes.contains } foreach { m => defaults += ("crypto.sym.mode" -> m) }

		// Also find a reasonable asymmetric-key cipher.
		preferred = List("RSA")

/* TODO: finish this!
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
		*/
		defaults
	}

	override def getBytes = Preferences.encode(getAll)
}

object Preferences { 
	/** Create a Preferences instance with auto-detected default settings. */
	def getDefaultPreferences = defaultPreferences getOrElse new Preferences(None)

	/** Load Preferences from the default filesystem location. */
	def loadFromDefaultLocation = create(Option(FileBackedPreferences.loadFromDefaultLocation))

	/** Create preferences with an explicitly-specified backing engine. */
	def create[T <: PreferenceStorageEngine](engine:Option[T]) = {
		val prefs = new Preferences(engine)
		if (defaultPreferences.isEmpty) { defaultPreferences = Option(prefs) }
		prefs
	}

	/** Wrap a String->Any mapping in a {@link Preferences} object.*/
	def wrap(map:Map[String,_]) = create(Option(PreferenceStorageEngine.wrap(map)))


	/** Generate byte encoding of {@link Preferences}. */
	def encode(snapshot:Map[String,_]): ByteBuffer = {
		var len = MAGIC.length + 4;  // Minimum length is magic + length representation (4B)
		for ((k,v) <- snapshot)
			len += (8 + k.length + v.toString.length);

		val encoded = ByteBuffer.allocate(len)
		encoded.put(MAGIC.toArray);
		encoded.putInt(snapshot.size());

		for ((key, value) <- snapshot)
		{
			encoded.putInt(key.length)
			encoded.put(key.getBytes)

			encoded.putInt(value.toString.length)
			encoded.put(value.toString.getBytes)
		}

		encoded.flip
		encoded
	}

	/** Parse raw bytes into {@link Preferences}. */
	def parse(bytes:ByteBuffer): Map[String,String] = {
		val magic = {
			val a = new Array[Byte](MAGIC.length)
			bytes.get(a)
			List.fromArray(a)
		}
		if (magic != MAGIC) throw new IOException("Incorrect Preferences magic");

		val values = MutableMap[String,String]()
		val entries = bytes.getInt()
		for (i <- 0 to entries - 1) {
			val keylen = bytes.getInt
			val key = new Array[Byte](keylen)
			bytes.get(key)

			val valuelen = bytes.getInt
			val value = new Array[Byte](valuelen)
			bytes.get(value)

			values += (new String(key) -> new String(value))
		}

		values
	}


	/** Magic bytes used to identify encoded {@link Preferences}: "FOOTOPTS" => "F0070475". */
	private val MAGIC = List(0xF0, 0x07, 0x04, 0x75) map { _.toByte }

	private val log = Logger.getLogger(Preferences.getClass.getCanonicalName)

	private var defaultPreferences:Option[Preferences] = None
}


/**
 * Preferences backed by a Java Properties file, suitable for most platforms.
 *
 * Some platforms (e.g. Android) may prefer platform-specific mechanisms.
 */
final class FileBackedPreferences(properties:java.util.Properties, configFile:java.io.File)
    extends PreferenceStorageEngine
	with java.io.Flushable with ModifiablePreferences {

	// PreferenceStorageEngine implementation
	private[core] override def getAll = com.google.common.collect.Maps.fromProperties(properties)
	override def getRaw(name:String) = Option(properties.getProperty(name))

	// ModifiablePreferences implementation
	override def set(key:String, value:String) = synchronized {
		properties.setProperty(key, value)
		dirty = true
		notifyAll()
		this
	}
	override def set(key:String, value:Boolean) = set(key, value)
	override def set(key:String, value:Int)     = set(key, value)
	override def set(key:String, value:Float)   = set(key, value)
	def set[T](key:String, value:T):FileBackedPreferences = set(key, value.toString)

	// Flushable implementation
	def flush = synchronized {
		if (dirty) {
			val comment = "Configuration options, auto-saved " +
					java.text.DateFormat.getDateInstance().format(new java.util.Date())

			properties.store(new java.io.FileOutputStream(configFile), comment)
			dirty = false
		}
	}

	// Object override
	override def finalize = { flush; super.finalize }


	/** Have any options changed? */
	private var dirty = false
}

final object FileBackedPreferences {
	/** The key used to store the cache directory in Preferences. */
	val CACHE_DIR_KEY = "footlights.cachedir"

	/** The key used to store the location of the Keychain. */
	val KEYCHAIN_KEY = "footlights.keychain"

	/** Path separator ('/' on UNIX, '\' on Windows). */
	val SEP = System getProperty("file.separator")

	/** The user's home directory. */
	val HOME = System getProperty("user.home")

	def loadFromDefaultLocation = load(openOrCreateConfigFile("Footlights"))

	/** Load the preferences in a given (local) file. */
	def load(file:java.io.File) = {
		val properties = new java.util.Properties

		if (file.exists) properties.load(new java.io.FileInputStream(file))
		else file.createNewFile()

		if (!properties.containsKey(CACHE_DIR_KEY))
			properties.setProperty(CACHE_DIR_KEY, file.getParent + SEP + "cache")

		if (!properties.containsKey(KEYCHAIN_KEY))
			properties.setProperty(KEYCHAIN_KEY, file.getParent + SEP + "keychain")

		new FileBackedPreferences(properties, file)
	}

	/** Create the configuration dir and file. */
	private def openOrCreateConfigFile(name:String) = {
		val homeDir = HOME + SEP + (
				if (System.getProperty("os.name").contains("Windows"))
					"Application Support" + SEP + "Footlights"
				else ".footlights"
			)

		// Ensure that the config dir exists.
		val dir = new java.io.File(homeDir)
		dir.mkdirs

		new java.io.File(dir, name + ".properties")
	}

}

}
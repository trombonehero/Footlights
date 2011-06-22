package me.footlights.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import me.footlights.core.Preferences;

import com.google.inject.AbstractModule;


/** Guice configuration for a Footlights web app. */
public class WebAppGuiceModule extends AbstractModule
{
	@Override
	protected void configure()
	{
		final Preferences preferences;

		try { preferences = Preferences.loadFromDefaultLocation(); }
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error loading Preferences", e);
			throw new RuntimeException(e);
		}

		bind(Preferences.class).toInstance(preferences);
		bind(Uploader.class).to(AmazonUploader.class);
	}

	private static final Logger log = Logger.getLogger(WebAppGuiceModule.class.getName());
}

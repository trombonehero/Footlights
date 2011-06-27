package me.footlights.core.plugin;

import java.net.URI;


/** A problem loading a {@link Plugin}. */
public class PluginLoadException extends Exception
{
	public PluginLoadException(URI uri, Throwable cause)
	{
		super("Failed to load plugin from '" + uri + "'", cause);
	}

	private static final long serialVersionUID
		= ("27 Jun 2011 1736h" + PluginLoadException.class.getCanonicalName()).hashCode();
}

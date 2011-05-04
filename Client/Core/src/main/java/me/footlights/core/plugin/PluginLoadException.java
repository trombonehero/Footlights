package me.footlights.core.plugin;


/** There was a problem loading a plugin */
public class PluginLoadException extends Exception
{
	public PluginLoadException(String url, Throwable cause)
	{
		super("Failed to load plugin from URL '" + url + "'", cause);
	}

	private static final long serialVersionUID
		= "footlights.core.plugin.PluginLoadException@2010-02-12/1438h".hashCode();
}

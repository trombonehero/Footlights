package me.footlights.core.plugin;


/** An error has occurred in the execution of a {@link Plugin}. */
public class PluginException extends RuntimeException
{
	public PluginException(String message, Throwable t)
	{
		super(message, t);
	}

	public PluginException(String message)
	{
		super(message);
	}

	private static final long serialVersionUID =
		("27 Jun 2011 1726h" + PluginException.class.getCanonicalName()).hashCode();
}

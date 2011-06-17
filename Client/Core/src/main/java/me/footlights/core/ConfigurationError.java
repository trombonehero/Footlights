package me.footlights.core;


/** A problem with the Footlights configuration. */
public class ConfigurationError extends RuntimeException
{
	public ConfigurationError(Throwable t)
	{
		super(t);
	}

	public ConfigurationError(String message)
	{
		super(message);
	}

	private static final long serialVersionUID =
		("27 Apr 2011 1331h" + ConfigurationError.class.getCanonicalName())
		.hashCode();
}

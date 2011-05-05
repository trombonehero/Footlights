package me.footlights.core;


/** Something is missing. */
public class MissingParameterException extends Exception
{
	public MissingParameterException(String parameterName)
	{
		super("Missing parameter: " + parameterName);
	}


	private static final long serialVersionUID =
		("5 May 2011 1500h" + MissingParameterException.class.getCanonicalName())
		.hashCode();	
}

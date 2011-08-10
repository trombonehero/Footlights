package me.footlights.ui.web;


/** A request from the Web client is invalid in some way. */
public class InvalidRequestException extends Exception
{
	public InvalidRequestException(String request, String error)
	{
		super("Invalid request \"" + request + "\": " + error);
	}


	private static final long serialVersionUID =
		("2011-01-20" + InvalidRequestException.class.getName()).hashCode();
}

package me.footlights.core.data;


/**
 * Incorrectly-formatted data bytes.
 * @author Jonathan Anderson (jon@footlights.me)
 */
public class FormatException extends java.io.IOException
{
	public FormatException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public FormatException(String message) { super(message); }

	private static final long serialVersionUID =
		"footlights.core.data.FormatException v1".hashCode();
}

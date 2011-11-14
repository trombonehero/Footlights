package me.footlights.core;


/**
 * The programmer has done something seriously wrong.
 *
 * This might include a failure to verify preconditions, a failure to correctly
 * anticipate runtime exceptions, etc.
 */
public class ProgrammerError extends Error
{
	public ProgrammerError(String message, Throwable cause) { super(message, cause); }
	public ProgrammerError(String message) { this(message, null); }

	private static final long serialVersionUID =
		(ProgrammerError.class.getCanonicalName() + "10 Nov 2011 1427 EST").hashCode();
}

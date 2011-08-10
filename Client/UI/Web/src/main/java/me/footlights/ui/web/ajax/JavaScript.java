package me.footlights.ui.web.ajax;

/** Some JavaScript code (guarantee closure?). */
class JavaScript
{
	JavaScript()
	{
		builder = new StringBuilder();
		frozen = null;
	}

	/** JavaScript for an Ajax call. */
	public static JavaScript ajax(String code)
	{
		return new JavaScript()
			.append("context.ajax('")
			.append(JavaScript.sanitizeText(code))
			.append("')");
	}

	/** Make a string safe to put within single quotes. */
	public static String sanitizeText(String input)
	{
		return input.replace("'", "\\'").replace("\n", "\\n");
	}

	public JavaScript append(JavaScript code)
	{
		append(code.exec());
		return this;
	}

	public JavaScript append(String code)
	{
		if (frozen != null)
			throw new UnsupportedOperationException(
				"append()'ing to a frozen JavaScript");

		builder.append(code);
		return this;
	}

	String asScript() { return code(); }
	String asFunction() { return "(function(){" + code() + "})"; }
	String exec() { return asFunction() + "();"; }

	private String code()
	{
		// TODO(jon): sanitization?
		if (frozen == null) frozen = builder.toString();
		return frozen;
	}

	private final StringBuilder builder;
	private String frozen;
}

package me.footlights.ui.web.ajax;


/** A response to an Ajax command. */
class AjaxResponse
{
	/** Type of response - different types are handled differently by the JS. */
	public enum Type
	{
		CODE("code"),
		ERROR("error");

		Type(String typeString) { this.typeString = typeString; }
		@Override public String toString() { return typeString; }
		private String typeString;
	}

	final Type type;
	final String context;
	final String content;

	public static class Builder
	{
		public AjaxResponse build()
		{
			if (type == null)
				throw new NullPointerException("'type' is null");

			if (context == null)
				throw new NullPointerException("'context' is null");

			if (content == null)
				throw new NullPointerException("'content' is null");

			return new AjaxResponse(type, context, content.toString());
		}

		public Builder setType(Type t) { type = t; return this; }
		public Builder setContext(String c) { context = c; return this; }
		public Builder append(String s) { content.append(s); return this; }
		
		private Builder() { content = new StringBuilder(); }

		private Type type;
		private String context;
		private StringBuilder content;
	}

	public static Builder newBuilder() { return new Builder(); }
	
	public String toXML()
	{
		String safeContent = content;

		safeContent = safeContent.replace("<", "%3C");
		safeContent = safeContent.replace(">", "%3E");

		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<response>\n");
		sb.append("\t<type>" + type + "</type>\n");
		sb.append("\t<context>" + context + "</context>\n");
		sb.append("\t<content>" + content + "</content>\n");
		sb.append("</response>");

		return sb.toString();
	}

	private AjaxResponse(Type type, String context, String content)
	{
		this.type = type;
		this.context = context;
		this.content = content;
	}
}

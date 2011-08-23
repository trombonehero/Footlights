/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.footlights.ui.web;


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

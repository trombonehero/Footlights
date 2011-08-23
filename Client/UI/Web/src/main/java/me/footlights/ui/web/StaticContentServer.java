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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Pattern;


/** Code to serve static plugin content. */
class StaticContentServer implements WebServer
{
	StaticContentServer(Map<String,? extends Object> plugins)
	{
		this.plugins = plugins;
	}

	@Override public String name() { return "static content server"; }

	@Override public Response handle(Request request) throws SecurityException
	{
		String path = request.path();
		if(path.contains(".."))
			throw new SecurityException(
				"The request path '" + path + "' contains '..'");

		if(path.equals("/")) path = "index.html";
		else if (path.startsWith("/")) path = path.substring(1);
		InputStream in = getClass().getResourceAsStream(path);

		// TODO: serve static content from plugins

		if(in == null)
			return Response.error(new FileNotFoundException("Unable to find '" + path + "'"));

		return Response.newBuilder()
			.setResponse(mimeType(path), in)
			.build();
	}

	/** Guess the MIME type for static content. */
	private String mimeType(String path)
	{
		if(Pattern.matches(".*\\.css", path))
			return "text/css";

		else if(Pattern.matches(".*\\.gif", path))
			return "image/gif";

		else if(Pattern.matches(".*\\.html", path))
			return "text/html";

		else if(Pattern.matches(".*\\.jpe?g", path))
			return "image/jpeg";

		else if(Pattern.matches(".*\\.js", path))
			return "text/javascript";

		else if(Pattern.matches(".*\\.png", path))
			return "image/png";

		return "text/xml";
	}

	private Map<String,? extends Object> plugins;
}

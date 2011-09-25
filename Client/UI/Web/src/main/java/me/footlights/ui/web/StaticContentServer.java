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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import me.footlights.core.plugin.PluginWrapper;
import me.footlights.plugin.WebRequest;


/** Code to serve static plugin content. */
class StaticContentServer implements WebServer
{
	StaticContentServer(Map<String,PluginWrapper> plugins)
	{
		paths = Maps.newHashMap();

		for (Map.Entry<String,PluginWrapper> plugin : plugins.entrySet())
			paths.put(plugin.getKey(), plugin.getValue().getClass());

		paths.put("footlights", getClass());

		// TODO: this is a temporary cheat for testing.
		paths.put("sandbox", getClass());
	}

	@Override public String name() { return "static content server"; }

	@Override public Response handle(final WebRequest request)
		throws FileNotFoundException, IOException, SecurityException
	{
		if (request.path().contains(".."))
			throw new SecurityException("'..' present in " + request);

		if (request.path().isEmpty())
			return Response.newBuilder()
				.setResponse(
						mimeType("index.html"),
						this.getClass().getResource("index.html").openStream())
				.build();

		Class<?> c = paths.get(request.prefix());
		if (c == null)
			throw new FileNotFoundException("No such directory '" + request.prefix() + "'");

		String path = request.shift().path();
		URL resource = c.getResource(request.shift().path());
		if (resource == null) throw new FileNotFoundException(path);

		java.io.File file = new java.io.File(resource.getFile());
		if (!file.isFile()) throw new FileNotFoundException(path);

		InputStream data = resource.openStream();
		if (data == null) throw new FileNotFoundException(path);

		return Response.newBuilder()
			.setResponse(mimeType(path), data)
			.build();
	}

	/** Guess the MIME type for static content. */
	private String mimeType(String path)
	{
		if (Pattern.matches(".*\\.css", path))
			return "text/css";

		else if (Pattern.matches(".*\\.gif", path))
			return "image/gif";

		else if (Pattern.matches(".*\\.html", path))
			return "text/html";

		else if (Pattern.matches(".*\\.jpe?g", path))
			return "image/jpeg";

		else if (Pattern.matches(".*\\.js", path))
			return "text/javascript";

		else if (Pattern.matches(".*\\.png", path))
			return "image/png";

		return "text/xml";
	}

	private final Map<String,Class<?>> paths;
}

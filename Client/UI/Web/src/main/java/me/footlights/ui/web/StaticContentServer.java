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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import me.footlights.api.WebRequest;
import me.footlights.core.apps.AppWrapper;


/** Code to serve static content. */
class StaticContentServer implements WebServer
{
	StaticContentServer(Map<String,AppWrapper> apps)
	{
		paths = new HashMap<String, Class<?>>();
		this.apps = apps;

		for (Map.Entry<String,AppWrapper> app : apps.entrySet())
			paths.put(app.getKey(), app.getValue().getClass());

		// TODO: this is a temporary cheat for testing.
		paths.put("sandbox", getClass());
	}

	@Override public String name() { return "static content server"; }

	@Override public Response handle(WebRequest request)
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

		final Class<?> resourceLoader;
		if (request.prefix().equals("app"))
		{
			request = request.shift();
			AppWrapper app = apps.get(java.net.URLDecoder.decode(request.prefix(), "utf-8"));
			if (app == null)
				throw new FileNotFoundException("No such app " + request.prefix());

			resourceLoader = app.app().getClass();
		}
		else if (request.prefix().equals("footlights"))
		{
			resourceLoader = this.getClass();
			if (resourceLoader == null)
				throw new FileNotFoundException("No such directory '" + request.prefix() + "'");
		}
		else throw new FileNotFoundException(
			"Static content filename must start with either 'app' or 'footlights'");

		String path = request.shift().path();
		URL url = resourceLoader.getResource(path);
		if (url == null) throw new FileNotFoundException(path);

		InputStream data = url.openStream();
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

		else if (Pattern.matches(".*\\.ttf", path))
			return "font/ttf";

		return "text/xml";
	}

	private final Map<String,Class<?>> paths;
	private final Map<String, AppWrapper> apps;
}

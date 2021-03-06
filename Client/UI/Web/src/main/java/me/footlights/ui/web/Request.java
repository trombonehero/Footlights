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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import me.footlights.api.WebRequest;
import me.footlights.core.ProgrammerError;


/** A request from the client, broken into path, query and fragment. */
public class Request implements WebRequest
{
	public static Request parse(String rawRequest) throws InvalidRequestException
	{
		if (!rawRequest.startsWith("GET "))
			throw new InvalidRequestException(rawRequest,
					"does not begin with \"GET \"");

		int newline = rawRequest.indexOf('\r');
		if (newline > 0) rawRequest = rawRequest.substring(0, newline);
		newline = rawRequest.indexOf('\n');
		if (newline > 0) rawRequest = rawRequest.substring(0, newline);

		if (!rawRequest.matches(".* HTTP/1.[01]$"))
			throw new InvalidRequestException(rawRequest,
					"does not end with \" HTTP/1.[01]\"");

		rawRequest = rawRequest
			.replaceFirst("^GET ", "")
			.replaceFirst(" HTTP/1.1", "");

		// Parse the fragment
		String fragment;
		String[] tmp = rawRequest.split("#");
		if (tmp.length > 1) fragment = tmp[1];
		else fragment = null;

		// Parse the query, if it exists
		Map<String,String> query = new LinkedHashMap<String,String>();
		tmp = tmp[0].split("\\?");
		if (tmp.length > 1)
		{
			for (String pair : tmp[1].split("&"))
			{
				String[] keyValue = pair.split("=");
				String value = (keyValue.length > 1) ? keyValue[1] : null;

				query.put(keyValue[0], value);
			}
		}

		String path = tmp[0].replaceFirst("^/*", "");

		return new Request(path, query, fragment);
	}


	/** Identify the "/prefix/of/the/path/to/foo.js". */
	public String prefix()
	{
		int slash = path.indexOf("/");
		final String encoded;

		if (slash == -1) encoded = path;
		else encoded = path.substring(0, slash);

		try { return URLDecoder.decode(encoded, "utf-8"); }
		catch (UnsupportedEncodingException e) { throw new ProgrammerError("UTF-8 error", e); }
	}

	/**
	 * Strip the path's prefix.
	 *
	 * e.g. "/static/path/to/foo.js" becomes "path/to/foo.js"
	 * @return
	 */
	public WebRequest shift()
	{
		if (path.isEmpty()) return this;

		int slash = path.indexOf("/");
		if (slash == -1) return this;

		String stripped = path.substring(slash + 1);
		return new Request(stripped, query, fragment);
	}

	private Request(String path, Map<String, String> query, String fragment)
	{
		this.path = path;
		this.query = query;
		this.fragment = fragment;
	}

	/** The path (everything before '&'). */
	public String path() { return path; }

	/** The query (foo=x, bar=y, etc., with no duplicate keys). Never null. */
	public Map<String, String> query()
	{
		return Collections.unmodifiableMap(query);
	}

	/** Everything after the '#'. */
	public String fragment() { return fragment; }

	@Override public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Request { path:'");
		sb.append(path);
		sb.append("', query:{ ");
		for (Map.Entry<String, String> entry : query.entrySet())
		{
			sb.append("'");
			sb.append(entry.getKey());
			sb.append("' => '");
			sb.append(entry.getValue());
			sb.append("' ");
		}
		sb.append("}, fragment:");
		if (fragment == null) sb.append(fragment);
		else
		{
			sb.append("'");
			sb.append(fragment);
			sb.append("'");
		}
		sb.append(" }");

		return sb.toString();
	}

	private final String path;
	private final Map<String, String> query;
	private final String fragment;
}

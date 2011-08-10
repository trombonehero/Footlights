package me.footlights.ui.web;

import static me.footlights.core.Log.log;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/** A request from the client, broken into path, query and fragment. */
public class Request
{
	/** Construct from an HTTP request string (no body). */
	Request(String rawRequest) throws InvalidRequestException
	{
		if(!rawRequest.startsWith("GET "))
			throw new InvalidRequestException(rawRequest,
					"does not begin with \"GET \"");

		if(!rawRequest.matches(".* HTTP/1.[01]$"))
			throw new InvalidRequestException(rawRequest,
					"does not end with \" HTTP/1.[01]\"");

		rawRequest = rawRequest
			.replaceFirst("^GET ", "")
			.replaceFirst(" HTTP/1.1", "");

		log("Raw request: " + rawRequest);

		// Parse the fragment
		String[] tmp = rawRequest.split("#");
		if (tmp.length > 1) fragment = tmp[1];
		else fragment = null;

		// Parse the query, if it exists
		query = new LinkedHashMap<String, String>();
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

		path = tmp[0];
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
		sb.append("Request { ");
		sb.append(path);
		sb.append("}");

		return sb.toString();
	}

	private final String path;
	private final Map<String, String> query;
	private final String fragment;
}

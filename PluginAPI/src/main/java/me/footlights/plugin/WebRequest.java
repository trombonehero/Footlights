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
package me.footlights.plugin;

import java.util.Map;


/** A request from the client, broken into path, query and fragment. */
public interface WebRequest
{
	/** Identify the "/prefix/of/the/path/to/foo.js". */
	public String prefix();

	/**
	 * Strip the path's prefix.
	 *
	 * e.g. "/static/path/to/foo.js" becomes "path/to/foo.js"
	 */
	public WebRequest shift();

	/** The path (everything before '&'). */
	public String path();

	/** The query (foo=x, bar=y, etc., with no duplicate keys). Never null. */
	public Map<String, String> query();

	/** Everything after the '#'. */
	public String fragment();
}

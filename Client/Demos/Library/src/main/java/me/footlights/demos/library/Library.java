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
package me.footlights.demos.library;

import me.footlights.api.ajax.JSON;


/**
 * Contains methods that might be useful to an app.
 *
 * @author Jonathan Anderson <jon@footlights.me>
 */
public class Library
{
	public static String version() { return "1.0-alpha0"; }
	public String method() { return NAME + ": regular method"; }
	public JSON json(int foo, String bar)
	{
		return new JSON().plus("int echo", foo).plus("String echo", bar);
	}

	private static String NAME = Library.class.getSimpleName();
}

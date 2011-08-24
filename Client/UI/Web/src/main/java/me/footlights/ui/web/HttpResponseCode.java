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


/** HTTP response codes and the standard messages that go with them. */
enum HttpResponseCode
{
	OK                (200, "OK"),
	FORBIDDEN         (403, "Forbidden"),
	FILE_NOT_FOUND    (404, "File Not Found"),
	OTHER_ERROR       (500, "Internal Server Error"),
	;

	HttpResponseCode(int code, String message)
	{
		this.code = code;
		this.message = message;
	}

	@Override public String toString() { return code + " " + message; }

	private final int code;
	private final String message;
}

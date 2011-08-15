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


/** A request from the Web client is invalid in some way. */
public class InvalidRequestException extends Exception
{
	public InvalidRequestException(String request, String error)
	{
		super("Invalid request \"" + request + "\": " + error);
	}


	private static final long serialVersionUID =
		("2011-01-20" + InvalidRequestException.class.getName()).hashCode();
}

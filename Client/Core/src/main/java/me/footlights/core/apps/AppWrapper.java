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
package me.footlights.core.apps;

import java.net.URI;

import me.footlights.api.Application;



/** Wrapper for applications; ensures consistent exception handling */
public final class AppWrapper
{
	/** Constructor */
	public AppWrapper(String name, URI url, Application app)
	{
		this.name = name;
		this.url = url;
		this.app = app;
	}


	public final String getName() { return name; }
	URI getOrigin() { return url; }
	public Application getApp() { return app; }


	/** The actual app. */
	private final Application app;

	/** The human-readable name that we know the app by. */
	private final String name;

	/** Where the application came from. */
	private final URI url;
}

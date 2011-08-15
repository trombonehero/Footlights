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
package me.footlights.core.data.store;

import java.io.IOException;
import java.util.AbstractCollection;


/**
 * A store which is in some sense "local" (e.g. in memory, on disk), so the
 * contents of the directory are available to be listed.
 */
public abstract class LocalStore extends Store
{
	public LocalStore(Store cache)
	{
		super(cache);
	}

	/**
	 * List the blocks that are stored here.
	 */
	public abstract AbstractCollection<String> list() throws IOException;
}

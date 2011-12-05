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
package me.footlights.plugin.ajax;

import me.footlights.plugin.WebRequest;



/** An object which can process Ajax requests. */
public interface AjaxHandler
{
	/**
	 * Handle an Ajax {@link WebRequest}.
	 * 
	 * @return {@link JavaScript} code to be returned to the sandbox
	 *
	 * @throws java.io.FileNotFoundException if the request's "path" can never be handled
	 * @throws SecurityException if TODO
	 * @throws Throwable as a last resort; this method may throw anything it likes
	 */
	public JavaScript service(WebRequest request)
		throws java.io.FileNotFoundException, SecurityException, Throwable;
}

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


/**
 * An Ajax response (JSON, XML, JavaScript, ...).
 *
 * It's best to use the provided Response subclasses. A plugin can always define a new subclass,
 * but the browser executive won't know what to do with it unless it declares a "known" MIME type. 
 */
public interface AjaxResponse
{
	/** Standard MIME type (e.g. "text/javascript"). */
	public String mimeType();

	/** The data itself (code, XML, raw string data, ...). */
	public java.io.InputStream data();
}

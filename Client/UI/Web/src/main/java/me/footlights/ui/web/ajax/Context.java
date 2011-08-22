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
package me.footlights.ui.web.ajax;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import me.footlights.ui.web.Request;


/** Handle to a client-side context (ECMAScript sandbox or 'window'). */ 
class Context
{
	Context(String name)
	{
		this.name = name;
		this.handlers = new LinkedHashMap<String, AjaxHandler>();
	}

	final AjaxResponse service(Request request)
	{
		AjaxResponse.Builder builder =
			AjaxResponse.newBuilder()
				.setType(AjaxResponse.Type.CODE)
				.setContext(this.name);

		try
		{
			AjaxHandler handler = handlers.get(
					request.path()
						.replaceFirst("^/", "")
						.split("%20")[0]);

			if (handler == null)
				throw new IllegalArgumentException(
					"No '" + request.path() + "' in context " + this);

			builder.append(handler.service(request).exec());
		}
		catch (Throwable t)
		{
			builder
				.setType(AjaxResponse.Type.ERROR)
				.append("Java error:\n");

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintWriter writer = new PrintWriter(baos);
			t.printStackTrace(writer);
			writer.flush();

			builder.append(baos.toString());
		} 

		return builder.build();
	}

	synchronized
	protected final Context register(String request, AjaxHandler handler)
	{
		if (handlers.containsKey(request))
			throw new IllegalArgumentException(
				this + " already has a handler registered for \"" + request
					+ "\" requests");

		handlers.put(request, handler);
		return this;
	}


	@Override public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("Context { name: '");
		sb.append(name);
		sb.append("', handlers: [ ");
		for (Map.Entry<String, AjaxHandler> handler : handlers.entrySet())
		{
			sb.append("'");
			sb.append(handler.getKey());
			sb.append("' ");
		}
		sb.append("] }");

		return sb.toString();
	}


	/** Context name, as understood by the ECMAScript. */
	final String name;

	/** Objects which handle requests. */
	private final Map<String, AjaxHandler> handlers;
}

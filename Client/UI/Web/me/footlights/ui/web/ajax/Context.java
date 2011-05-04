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


	/** Context name, as understood by the ECMAScript. */
	final String name;

	/** Objects which handle requests. */
	private final Map<String, AjaxHandler> handlers;
}

package me.footlights.ui.web;

import java.io.FileNotFoundException;
import java.io.IOException;


/** An {@link AjaxHandler} that communicates with test JavaScript. */
public class TestHandler implements AjaxHandler
{
	@Override
	public JavaScript service(Request request)
		throws FileNotFoundException, IOException, SecurityException
	{
		return new JavaScript()
			.append("context.log('This is a message in response to the request \"")
			.append(request.path())
			.append("\"');");
	}
}

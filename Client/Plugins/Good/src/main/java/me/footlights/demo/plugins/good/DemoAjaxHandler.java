package me.footlights.demo.plugins.good;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.footlights.plugin.AjaxHandler;
import me.footlights.plugin.File;
import me.footlights.plugin.JavaScript;
import me.footlights.plugin.KernelInterface;
import me.footlights.plugin.WebRequest;


/** A demonstration of what a {@link Plugin}'s {@link AjaxHandler} can do. */
class DemoAjaxHandler implements AjaxHandler
{
	enum AjaxRequest
	{
		INIT,
		TEST_METHODS,
		SYSCALL,
		CONTENT,
		ALL_DONE,
		CLICKED,
	}

	DemoAjaxHandler(KernelInterface kernel, Logger log)
	{
		this.kernel = kernel;
		this.log = log;
	}

	@Override
	public JavaScript service(WebRequest request)
	{
		JavaScript response = new JavaScript();

		response.append(hr());
		response.append(makeDiv("Acting in response to request '" + request.path() + "'"));

		final AjaxRequest ajax;
		try { ajax = AjaxRequest.valueOf(request.prefix().toUpperCase()); }
		catch (IllegalArgumentException e)
		{
			return response.append(
				"log.log('Unknown request \\'" + JavaScript.sanitizeText(request.path()) + "\\'')");
		}

		switch (ajax)
		{
			case INIT:
				response.append(makeDiv("Initializing well-behaved plugin."));
				response.append(makeDiv("The time is " + new Date()));
				response.append(ajax(AjaxRequest.TEST_METHODS.name()));
				break;

			case TEST_METHODS:
				response.append(
					makeDiv("Test static method in the Helper class... " + Helper.staticHelp()));
					response.append(makeDiv("Ok, that was fine. Now a constructor... "));
					Helper h = new Helper();
					response.append(makeDiv("And a regular method... "+ h.help()));
					response.append(ajax(AjaxRequest.SYSCALL.name()));
					break;

			case SYSCALL:
				response.append(makeDiv("Finally, do a 'syscall'..."));
				if (kernel == null)
					response.append(makeDiv("but we can't! our kernel reference is null."));
				else
				{
					try
					{
						File file = kernel.save(ByteBuffer.wrap("Hello, world!".getBytes()));
						response.append(makeDiv("saved file: " + file));
					}
					catch (IOException e)
					{
						response.append(makeDiv("Error saving data: " + e));
						log.log(Level.SEVERE, "Error saving data", e);
					}
				}

				response.append(ajax(AjaxRequest.CONTENT.name()));
				break;

			case CONTENT:
				response.append("context.load('test.js');");
				/*
				 * TODO: fix ajax.js so that we have more than one XHR object.
				 *
				 * Until this is done, sending ajax('ALL_DONE') will cancel load('test.js'),
				 * leading to a rather less convincing demo.
				 */
//				response.append(ajax(AjaxRequest.ALL_DONE.name()));
//				break;

			case ALL_DONE:
				response.append(makeDiv("The plugin works!."));
				break;

			case CLICKED:
				response.append("context.log('Clicked \\'" + request.shift().path() + "\\'');");
				break;
		}

		return response;
	}

	private static JavaScript hr()
	{
		return new JavaScript().append("context.root.appendElement('hr')");
	}

	private static JavaScript makeDiv(String text)
	{
		return new JavaScript()
		.append("context.root.appendElement('div')")
		.append(".appendText('")
		.append(JavaScript.sanitizeText(text))
		.append("')");
	}

	private static JavaScript ajax(String command)
	{
		return new JavaScript()
			.append("context.ajax('")
			.append(JavaScript.sanitizeText(command))
			.append("')");
	}

	private final KernelInterface kernel;
	private final Logger log;
}

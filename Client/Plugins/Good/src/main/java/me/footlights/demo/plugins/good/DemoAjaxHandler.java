package me.footlights.demo.plugins.good;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.footlights.File;
import me.footlights.plugin.AjaxHandler;
import me.footlights.plugin.JavaScript;
import me.footlights.plugin.KernelInterface;
import me.footlights.plugin.WebRequest;


/** A demonstration of what a {@link Plugin}'s {@link AjaxHandler} can do. */
class DemoAjaxHandler implements AjaxHandler
{
	DemoAjaxHandler(KernelInterface kernel, Logger log)
	{
		this.kernel = kernel;
		this.log = log;
	}

	@Override
	public JavaScript service(WebRequest request)
	{
		JavaScript response = new JavaScript();

		response.append(makeDiv("I am a well-behaved plugin."));
		response.append(makeDiv("The time is " + new Date()));

		response.append(
			makeDiv(
				"Let's test a static method in the Helper class... " + Helper.staticHelp()));

		response.append(makeDiv("Ok, that was fine. Now a constructor... "));
		Helper h = new Helper();

		response.append(makeDiv("And a regular method... "+ h.help()));

		response.append(makeDiv("Finally, do a 'syscall'..."));
		if (kernel == null) response.append(makeDiv("but we can't! our kernel reference is null."));
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

		response.append(makeDiv("The plugin works!."));

		return response;
	}

	private static JavaScript makeDiv(String text)
	{
		return new JavaScript()
		.append("context.root.appendElement('div')")
		.append(".appendText('")
		.append(JavaScript.sanitizeText(text))
		.append("')");
	}

	private final KernelInterface kernel;
	private final Logger log;
}

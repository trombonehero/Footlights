package me.footlights.demo.plugins.wicked;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.logging.Logger;

import me.footlights.plugin.AjaxHandler;
import me.footlights.plugin.JavaScript;
import me.footlights.plugin.KernelInterface;
import me.footlights.plugin.WebRequest;

import static me.footlights.demo.plugins.wicked.EvilAjaxHandler.AjaxRequest.*;


/** A demonstration of what a {@link Plugin}'s {@link AjaxHandler} can do. */
class EvilAjaxHandler implements AjaxHandler
{
	enum AjaxRequest
	{
		INIT,
		FAKE_CORE,
		SEALED_PACKAGE,
		WICKED_WEBSITE,
		CORE_ACCESS,
		NEW_CLASSLOADER,
		LOAD_PLUGIN,
		JS_HACKS,
		CLICKED,
		ALL_DONE,
	}

	EvilAjaxHandler(KernelInterface kernel, Logger log)
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
				"context.log('Unknown request \\'" + JavaScript.sanitizeText(request.path()) + "\\'')");
		}

		switch (ajax)
		{
			case INIT:
				response.append(ajax(FAKE_CORE.name()));
				break;

			case FAKE_CORE:
				response.append(makeDiv("Attempting to load a fake 'core' class via URL... "));
				try
				{
					String url = 
						"jar:file:///Users/jon/Documents/School/Research/Social Networks/Footlights/Client/plugins/wicked/wicked.jar!/footlights.core.Test";
					kernel.getClass().getClassLoader().loadClass(url);

					throw new SecurityException(
							"Encapsulation failure: loaded fake 'core' class");
				}
				catch(AccessControlException e) { response.append(makeDiv("denied.")); }
				catch(ClassNotFoundException e) { response.append(makeDiv("not found.")); }
				response.append(ajax(SEALED_PACKAGE.name()));
				break;

			case SEALED_PACKAGE:
				response.append(makeDiv("Attempting to load a class in[to] a sealed package... "));
				try
				{
					response.append(makeDiv(me.footlights.core.Test.test()));

					throw new SecurityException(
							"Encapsulation failure: loaded footlights.core.Test");
				}
				catch(AccessControlException e) { response.append(makeDiv("denied.")); }
				catch(NoClassDefFoundError e) { response.append(makeDiv("not found.")); }
				response.append(ajax(WICKED_WEBSITE.name()));
				break;

			case WICKED_WEBSITE:
				response.append(makeDiv("Trying to connect to a wicked website... "));
				try
				{
					new Socket("www-dyn.cl.cam.ac.uk", 80);
					throw new SecurityException(
							"Encapsulation failure: connection succeeded");
				}
				catch(AccessControlException e) { response.append(makeDiv("denied.")); }
				catch(IOException e) { throw new Error(e); }
				response.append(ajax(CORE_ACCESS.name()));
				break;

			case CORE_ACCESS:
				response.append(makeDiv("Check access to footlights.core... "));
				try
				{
					AccessController.checkPermission(
						new RuntimePermission("accessClassInPackage.footlights.core"));
					throw new SecurityException("Access granted to footlights.core");
				}
				catch(AccessControlException e) { response.append(makeDiv("denied.")); }
				response.append(ajax(NEW_CLASSLOADER.name()));
				break;

			case NEW_CLASSLOADER:
				response.append(makeDiv("Check ability to create a new ClassLoader... "));
				try
				{
					AccessController.checkPermission(
						new RuntimePermission("createClassLoader"));

					throw new SecurityException(
							"Encapsulation failure: permission to create ClassLoader");
				}
				catch(AccessControlException e) { response.append(makeDiv("denied.")); }
				response.append(ajax(LOAD_PLUGIN.name()));
				break;

			case LOAD_PLUGIN:
				response.append(makeDiv("Trying to instantiate another plugin... "));
				try
				{
					Class<?> c = kernel.getClass().getClassLoader().loadClass(
						"jar:file:///home/jra40/Research/Social Networks/Footlights/Client/plugins/good/good.jar!/footlights.demo.plugins.good.Plugin");

					Class<?> classes[] = new Class[] { c };

					Method setOut = c.getMethod("setOutputStream", classes);
					Method run = c.getMethod("run", (Class<?>[]) null);

					Object p = c.newInstance();
					setOut.invoke(p, new Object[] { log });
					run.invoke(p, (Object[]) null);

					throw new SecurityException(
							"Encapsulation failure: ran another plugin");
				}
				catch(AccessControlException e) { response.append(makeDiv("denied.")); }
				catch(ClassNotFoundException e)
				{
					response.append(makeDiv("not found. (not denied, just not found)"));
				}
				catch(Exception e) { throw new Error(e); }
				response.append(ajax(JS_HACKS.name()));
				break;

			case JS_HACKS:
				response.append("context.load('evil.js');");
				response.append(ajax(AjaxRequest.ALL_DONE.name()));
				break;

			case CLICKED:
				response.append("context.log('clicked \\'");
				response.append(JavaScript.sanitizeText(request.shift().path()));
				response.append("\\'');");

			case ALL_DONE:
				response.append(makeDiv("Failed to do anything wicked (hooray!)."));
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

package me.footlights.demos.wicked;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.logging.Logger;

import me.footlights.api.KernelInterface;
import me.footlights.api.WebRequest;
import me.footlights.api.ajax.AjaxHandler;
import me.footlights.api.ajax.Context;
import me.footlights.api.ajax.JavaScript;

import static me.footlights.demos.wicked.EvilAjaxHandler.AjaxRequest.*;


/** A demonstration of what a {@link Application}'s {@link AjaxHandler} can do. */
class EvilAjaxHandler extends Context
{
	enum AjaxRequest
	{
		INIT,
		FAKE_CORE,
		SEALED_PACKAGE,
		WICKED_WEBSITE,
		CORE_ACCESS,
		NEW_CLASSLOADER,
		LOAD_APPLICATION,
		JS_HACKS,
		CLICKED,
		ALL_DONE,
	}

	EvilAjaxHandler(final KernelInterface kernel, final Logger log)
	{
		register(INIT.name().toLowerCase(), new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
					throws FileNotFoundException, SecurityException, Throwable
				{
					return new JavaScript()
						.append(ajax(FAKE_CORE.name()));
				}
			});

		register(FAKE_CORE.name(), new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
					throws FileNotFoundException, SecurityException, Throwable
				{
					JavaScript response = new JavaScript()
						.append(openDiv())
						.appendText("Attempting to load a fake 'core' class via URL... ");

					try
					{
						String url = 
							"jar:file:///Users/jon/Footlights/Client/Demos/wicked/target/wicked.jar!/me.footlights.core.Test";
						kernel.getClass().getClassLoader().loadClass(url);

						throw new SecurityException(
							"Encapsulation failure: loaded fake 'core' class");
					}
					catch(AccessControlException e) { response.append("denied."); }
					catch(ClassNotFoundException e) { response.append("not found."); }

					return response
						.append(closeDiv())
						.append(ajax(SEALED_PACKAGE.name()));
				}
			});

		register(SEALED_PACKAGE.name(), new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
					throws FileNotFoundException, SecurityException, Throwable
				{
					JavaScript response = new JavaScript()
						.append(openDiv())
						.appendText("Attempting to load a class in[to] a sealed package... ");

					try
					{
						response.append(me.footlights.core.Test.test());

						throw new SecurityException(
								"Encapsulation failure: loaded footlights.core.Test");
					}
					catch(AccessControlException e) { response.append("denied."); }
					catch(NoClassDefFoundError e) { response.append("not found."); }

					return response.append(closeDiv()).append(ajax(WICKED_WEBSITE.name()));
				}
			});

		register(WICKED_WEBSITE.name(), new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
					throws FileNotFoundException, SecurityException, Throwable
				{
					JavaScript response = new JavaScript()
						.append(openDiv())
						.appendText("Trying to connect to a wicked website... ");

					try
					{
						new Socket("www-dyn.cl.cam.ac.uk", 80);
						throw new SecurityException(
								"Encapsulation failure: connection succeeded");
					}
					catch(AccessControlException e) { response.append("denied."); }
					catch(IOException e) { throw new Error(e); }

					return response
						.append(closeDiv())
						.append(ajax(CORE_ACCESS.name()));
				}
			});

		register(CORE_ACCESS.name(), new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
					throws FileNotFoundException, SecurityException, Throwable
				{
					JavaScript response = new JavaScript()
						.append(openDiv())
						.appendText("Check access to footlights.core... ");

					try
					{
						AccessController.checkPermission(
							new RuntimePermission("accessClassInPackage.me.footlights.core"));
						throw new SecurityException("Access granted to footlights.core");
					}
					catch(AccessControlException e) { response.appendText("denied."); }

					return response
						.append(closeDiv())
						.append(ajax(NEW_CLASSLOADER.name()));
				}
			});

		register(NEW_CLASSLOADER.name(), new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
					throws FileNotFoundException, SecurityException, Throwable
				{
					JavaScript response = new JavaScript()
						.append(openDiv())
						.appendText("Check ability to create a new ClassLoader... ");

					try
					{
						AccessController.checkPermission(
							new RuntimePermission("createClassLoader"));

						throw new SecurityException(
							"Encapsulation failure: permission to create ClassLoader");
					}
					catch(AccessControlException e) { response.appendText("denied."); }

					return response
						.append(closeDiv())
						.append(ajax(LOAD_APPLICATION.name()));
				}
			});

		register(LOAD_APPLICATION.name(), new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
					throws FileNotFoundException, SecurityException, Throwable
				{
					JavaScript response = new JavaScript()
						.append(openDiv())
						.appendText("Trying to instantiate another app... ");

					try
					{
						Class<?> c = kernel.getClass().getClassLoader().loadClass(
							"jar:file:///Users/jon/Footlights/Client/Demos/Basic/good.jar!/footlights.demos.good.GoodApp");

						Class<?> classes[] = new Class[] { c };

						Method setOut = c.getMethod("setOutputStream", classes);
						Method run = c.getMethod("run", (Class<?>[]) null);

						Object p = c.newInstance();
						setOut.invoke(p, new Object[] { log });
						run.invoke(p, (Object[]) null);

						throw new SecurityException(
							"Encapsulation failure: ran another application");
					}
					catch(AccessControlException e) { response.appendText("denied."); }
					catch(ClassNotFoundException e)
					{
						response.appendText("not found. (not denied, just not found)");
					}
					catch(Exception e) { throw new Error(e); }

					return response
						.append(closeDiv())
						.append(ajax(JS_HACKS.name()));
				}
			});

		register(JS_HACKS.name(), new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
					throws FileNotFoundException, SecurityException, Throwable
				{
					return new JavaScript()
						.append("context.load('evil.js');")
						.append(ajax(AjaxRequest.ALL_DONE.name()));
				}
			});

		register(ALL_DONE.name(), new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
					throws FileNotFoundException, SecurityException, Throwable
				{
					return new JavaScript()
						.append(openDiv())
						.appendText("Failed to do anything wicked (hooray!).")
						.append(closeDiv());
				}
			});

		register(CLICKED.name().toLowerCase(), new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
					throws FileNotFoundException, SecurityException, Throwable
				{
					return new JavaScript()
						.append("context.log('clicked \\'")
						.appendText(request.shift().path())
						.append("\\'');");
				}
			});
	}


	private static String openDiv()
	{
		return "context.root.appendElement('div').appendText('";
	}

	private static String closeDiv() { return "');"; }

	private static JavaScript ajax(String command)
	{
		return new JavaScript()
			.append("context.ajax('")
			.appendText(command)
			.append("')");
	}
}

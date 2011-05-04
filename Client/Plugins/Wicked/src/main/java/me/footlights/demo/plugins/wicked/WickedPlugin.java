package me.footlights.demo.plugins.wicked;


import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.AccessControlException;
import java.security.AccessController;


public class WickedPlugin extends me.footlights.core.plugin.Plugin
{
	public String name() { return "Wicked Plugin"; }
	public void setOutputStream(PrintWriter out) { this.output = out; }


	public void run() throws SecurityException
	{
		output.print("Attempting to load a fake 'core' class via URL... ");
		try
		{
			String url = 
				"jar:file:///Users/jon/Documents/School/Research/Social Networks/Footlights/Client/plugins/wicked/wicked.jar!/footlights.core.Test";
			getClass().getClassLoader().loadClass(url);

			throw new SecurityException(
					"Encapsulation failure: loaded fake 'core' class");
		}
		catch(AccessControlException e) { output.println("denied."); }
		catch(ClassNotFoundException e) { output.println("not found."); }


		output.print("Attempting to load a class in[to] a sealed package... ");
		try
		{
			output.println(me.footlights.core.Test.test());

			throw new SecurityException(
					"Encapsulation failure: loaded footlights.core.Test");
		}
		catch(AccessControlException e) { output.println("denied."); }
		catch(NoClassDefFoundError e) { output.println("not found."); }



		output.print("Trying to connect to a wicked website... ");
		try
		{
			new Socket("www-dyn.cl.cam.ac.uk", 80);
			throw new SecurityException(
					"Encapsulation failure: connection succeeded");
		}
		catch(AccessControlException e) { output.println("denied."); }
		catch(IOException e) { throw new Error(e); }



		output.print("Check access to footlights.core... ");
		try
		{
			AccessController.checkPermission(
				new RuntimePermission("accessClassInPackage.footlights.core"));
			throw new SecurityException("Access granted to footlights.core");
		}
		catch(AccessControlException e) { output.println("denied."); }



		output.print("Check ability to create a new ClassLoader... ");
		try
		{
			AccessController.checkPermission(
				new RuntimePermission("createClassLoader"));

			throw new SecurityException(
					"Encapsulation failure: permission to create ClassLoader");
		}
		catch(AccessControlException e) { output.println("denied."); }



		output.print("Trying to instantiate another plugin... ");
		try
		{
			Class<?> c = this.getClass().getClassLoader().loadClass(
				"jar:file:///home/jra40/Research/Social Networks/Footlights/Client/plugins/good/good.jar!/footlights.demo.plugins.good.Plugin");

			Class<?> classes[] = new Class[] { c };

			Method setOut = c.getMethod("setOutputStream", classes);
			Method run = c.getMethod("run", (Class<?>[]) null);

			Object p = c.newInstance();
			setOut.invoke(p, new Object[] { output });
			run.invoke(p, (Object[]) null);

			throw new SecurityException(
					"Encapsulation failure: ran another plugin");
		}
		catch(AccessControlException e) { output.println("denied."); }
		catch(Exception e) { throw new Error(e); }


		output.println("Failed to do anything wicked (hooray!).");
	}

	/** Output stream */
	private PrintWriter output;
}

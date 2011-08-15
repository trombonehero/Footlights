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
package me.footlights.demo.plugins.wicked;


import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.logging.Logger;

import me.footlights.plugin.KernelInterface;


public class WickedPlugin implements me.footlights.plugin.Plugin
{
	@Override public void run(KernelInterface kernel, Logger log) throws SecurityException
	{
		log.info("Attempting to load a fake 'core' class via URL... ");
		try
		{
			String url = 
				"jar:file:///Users/jon/Documents/School/Research/Social Networks/Footlights/Client/plugins/wicked/wicked.jar!/footlights.core.Test";
			getClass().getClassLoader().loadClass(url);

			throw new SecurityException(
					"Encapsulation failure: loaded fake 'core' class");
		}
		catch(AccessControlException e) { log.info("denied."); }
		catch(ClassNotFoundException e) { log.info("not found."); }


		log.info("Attempting to load a class in[to] a sealed package... ");
		try
		{
			log.info(me.footlights.core.Test.test());

			throw new SecurityException(
					"Encapsulation failure: loaded footlights.core.Test");
		}
		catch(AccessControlException e) { log.info("denied."); }
		catch(NoClassDefFoundError e) { log.info("not found."); }



		log.info("Trying to connect to a wicked website... ");
		try
		{
			new Socket("www-dyn.cl.cam.ac.uk", 80);
			throw new SecurityException(
					"Encapsulation failure: connection succeeded");
		}
		catch(AccessControlException e) { log.info("denied."); }
		catch(IOException e) { throw new Error(e); }



		log.info("Check access to footlights.core... ");
		try
		{
			AccessController.checkPermission(
				new RuntimePermission("accessClassInPackage.footlights.core"));
			throw new SecurityException("Access granted to footlights.core");
		}
		catch(AccessControlException e) { log.info("denied."); }



		log.info("Check ability to create a new ClassLoader... ");
		try
		{
			AccessController.checkPermission(
				new RuntimePermission("createClassLoader"));

			throw new SecurityException(
					"Encapsulation failure: permission to create ClassLoader");
		}
		catch(AccessControlException e) { log.info("denied."); }



		log.info("Trying to instantiate another plugin... ");
		try
		{
			Class<?> c = this.getClass().getClassLoader().loadClass(
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
		catch(AccessControlException e) { log.info("denied."); }
		catch(Exception e) { throw new Error(e); }


		log.info("Failed to do anything wicked (hooray!).");
	}
}

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
package me.footlights.ui.web;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import me.footlights.core.Footlights;


/** Acts as a master server for Basic UI, JavaScript and Ajax */
public class MasterServer implements Runnable, WebServer
{
	public MasterServer(int port, Footlights footlights,
		AjaxServer ajaxServer, StaticContentServer staticServer)
	{
		this.port = port;
		this.servers = Maps.newHashMap();

		servers.put("", staticServer);
		servers.put("static", staticServer);

		servers.put("ajax", ajaxServer);
	}


	@Override public String name() { return "Master"; }

	@Override public Response handle(Request request)
	{
		try { return servers.get(request.prefix()).handle(request.shift()); }
		catch(FileNotFoundException e) { return Response.error(e); }
		catch(Throwable t) { return Response.error(t); }
	}


	public void run()
	{
		log.entering(getClass().getName(), "run");

		try
		{
			ServerSocket serverSocket = new ServerSocket(port);

			done = false;
			while(!done)
			{
				log.fine("Waiting for connection...");
				Socket socket = serverSocket.accept();
				log.fine("Accepted conncetion from " + socket);

				BufferedReader in =
					new BufferedReader(
						new InputStreamReader(
							socket.getInputStream()));

				String rawRequest = in.readLine();
				if(rawRequest == null) continue;

				Request request = new Request(rawRequest);
				log.info("Request: " + request.toString());

				Response response = handle(request);
				if (response.isError())
					log.warning("Error handling " + request + ": " + response.statusMessage());

				try
				{
					response.write(socket.getOutputStream());
					log.finer("Sent response: " + response);
					socket.close();
				}
				catch(SocketException e)
				{
					log.log(Level.SEVERE, "Uncaught SocketException", e);
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Web UI dying:");
			e.printStackTrace(System.err);
		}
	}


	private static final Logger log = Logger.getLogger(MasterServer.class.getName());

	private final int port;
	private boolean done;

	private final Map<String,WebServer> servers;
}

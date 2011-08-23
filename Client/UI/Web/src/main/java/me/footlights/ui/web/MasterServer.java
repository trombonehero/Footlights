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

import static me.footlights.core.Log.log;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

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
		log("Starting...");

		try
		{
			ServerSocket serverSocket = new ServerSocket(port);

			done = false;
			while(!done)
			{
				log("Waiting for connection...");
				Socket socket = serverSocket.accept();
				log("Connection accepted.");

				BufferedReader in =
					new BufferedReader(
						new InputStreamReader(
							socket.getInputStream()));

				String rawRequest = in.readLine();
				if(rawRequest == null) continue;
				Response response = handle(new Request(rawRequest));

				try
				{
					response.write(socket.getOutputStream());
					log("Response sent");
	
					socket.close();
					log("Closed connection");
				}
				catch(SocketException e)
				{
					e.printStackTrace(System.err);  // TODO: better handling
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Web UI dying:");
			e.printStackTrace(System.err);
		}
	}


	private final int port;
	private boolean done;

	private final Map<String,WebServer> servers;
}

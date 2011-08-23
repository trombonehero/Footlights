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
import java.util.regex.*;

import me.footlights.core.Footlights;


/** Acts as a master server for Basic UI, JavaScript and Ajax */
public class MasterServer implements Runnable, WebServer
{
	public MasterServer(int port, Footlights footlights, AjaxServer ajaxServer)
	{
		this.port = port;
		this.ajaxServer = ajaxServer;
	}


	@Override public String name() { return "Master"; }
	@Override public String mimeType(String path)
	{
		if(Pattern.matches("/", path))
			return "text/html";

		if(Pattern.matches("/.*\\.css", path))
			return "text/css";

		else if(Pattern.matches("/.*\\.gif", path))
			return "image/gif";

		else if(Pattern.matches("/.*\\.html", path))
			return "text/html";

		else if(Pattern.matches("/.*\\.jpe?g", path))
			return "image/jpeg";

		else if(Pattern.matches("/.*\\.js", path))
			return "text/javascript";

		else if(Pattern.matches("/.*\\.png", path))
			return "image/png";

		return "text/xml";
	}

	@Override public InputStream handle(Request request)
		throws FileNotFoundException, SecurityException
	{
		String contentPattern = "/(.*\\.((css)|(html)|(ico)|(jpeg)|(js)))?";

		if(Pattern.matches(contentPattern, request.path()))		
			return findStaticContent(request.path());

		else
		{
			if(request.equals("shutdown")) done = true;
			return ajaxServer.handle(request);
		}
	}
	
	
	/** Find static content, e.g. HTML, CSS or images */
	public InputStream findStaticContent(String path)
		throws FileNotFoundException, SecurityException
	{
		if(path.contains(".."))
			throw new SecurityException(
				"The request path '" + path + "' contains '..'");

		if(path.equals("/")) path = "index.html";
		else if (path.startsWith("/")) path = path.substring(1);
		InputStream in = getClass().getResourceAsStream(path);

		if(in == null)
			throw new FileNotFoundException("Unable to find '" + path + "'");
		
		else return in;
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

				Response.Builder response = Response.newBuilder();

				try
				{
					Request request = new Request(rawRequest);
					response.setResponse(mimeType(request.path()), handle(request));
				}
				catch(FileNotFoundException e) { response.setError(e); }
				catch(Throwable t) { response.setError(t); }

				try
				{
					response.build().write(socket.getOutputStream());
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
	private final AjaxServer ajaxServer;
	private boolean done;
}

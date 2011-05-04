package me.footlights.ui.web;

import static me.footlights.core.Log.log;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.*;

import me.footlights.core.Footlights;
import me.footlights.ui.web.ajax.AjaxServer;


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
		InputStream in = getClass().getResourceAsStream("content/" + path);

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

				StringBuilder sb = new StringBuilder();
				InputStream content = null;

				try
				{
					Request request = new Request(rawRequest);
					content = handle(request);

					sb.append("HTTP/1.1 200 OK\n");
					sb.append("Content-Type: " + mimeType(request.path()) + "\n");
				}
				catch(FileNotFoundException e)
				{
					sb.append("HTTP/1.1 404 File Not Found\n");
				}
				catch(Throwable e)
				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					PrintWriter writer = new PrintWriter(baos);

					e.printStackTrace(writer);
					writer.flush();

					sb.append("HTTP/1.1 500 Server Error\n");
					sb.append("\n");
					sb.append("<html><body><pre>");
					sb.append(baos.toString());
					sb.append("</pre></body></html>");
				}

				sb.append("\n");

				try
				{
					OutputStream out = socket.getOutputStream();
					out.write(sb.toString().getBytes());
	
					if(content != null)
					{
						byte[] data = new byte[10240];
						while(true)
						{
							int bytes = content.read(data);
							
							if(bytes <= 0) break;
							else out.write(data, 0, bytes);
						}
					}
					out.flush();
	
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

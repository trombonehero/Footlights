package me.footlights.ui.web;

import java.io.*;


public interface WebServer
{
	/** The server's name (e.g. Ajax, JavaScript...). */
	public String name();

	/** The type of server we're running. */
	public String mimeType(String request);

	/** Handle a user request. */
	public InputStream handle(Request request)
		throws FileNotFoundException, SecurityException;
}

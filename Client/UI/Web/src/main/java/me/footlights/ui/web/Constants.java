package me.footlights.ui.web;

public class Constants
{
	public static String BASE_URL =
		"file://"
		+ System.getProperty("java.class.path").replaceFirst("Client/.*", "");

	public static String PLUGIN_URL = BASE_URL + "Client/JARs/Plugins";

	public static int WEB_PORT = 4567;
}

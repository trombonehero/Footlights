package me.footlights.core.plugin;

import java.io.*;



/** Wrapper for plugins; ensures consistent exception handling */
public class PluginWrapper extends Thread
{
	/** Constructor */
	public PluginWrapper(String url, Plugin plugin)
	{
		super(plugin.name());

		this.plugin = plugin;
		this.url = url;
		this.output = new ByteArrayOutputStream();
		this.outWriter = new PrintWriter(this.output);

		plugin.setOutputStream(outWriter);
	}


	public Plugin wrapped() { return plugin; }
	public final String url() { return url; }


	/** Run the plugin, trapping exceptions if necessary */
	public void run()
	{
		try
		{
			error = null;

			output.reset();
			plugin.run();
			outWriter.flush();
		}
		catch(Throwable t) { error = t; }
	}


	public String output() throws Throwable
	{
		if(error != null) throw error;
		else return output.toString();
	}


	/** The actual plugin */
	private Plugin plugin;

	/** Where the plugin came from */
	private String url;

	/** Any error encountered while running the plugin */
	private Throwable error;

	/** The plugin's output */
	private ByteArrayOutputStream output;

	/** For writing to the output stream */
	private PrintWriter outWriter;
}

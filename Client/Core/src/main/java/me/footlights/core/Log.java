package me.footlights.core;

import java.io.PrintStream;


public class Log
{
	public static void log(String line)
	{
		instance().logStatus(line);
	}


	public void logStatus(String line)
	{
		System.out.println(line);
	}


	public static Log instance()
	{
		if(instance == null) instance = new Log();
		return instance;
	}

	public void setStream(PrintStream stream) { outputStream = stream; }
	public PrintStream stream() { return outputStream; }

	protected Log()
	{
		outputStream = System.out;

//		footlights.core.ui.StatusDialog.status("Redirecting stdout...");
//		outputStream = footlights.core.ui.StatusDialog.instance().stream();	
	}

	private static Log instance;
	private PrintStream outputStream;
}

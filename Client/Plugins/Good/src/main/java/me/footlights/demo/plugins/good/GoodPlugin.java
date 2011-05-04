package me.footlights.demo.plugins.good;

import java.io.PrintWriter;
import java.util.Date;


/**
 * A well-behaved plugin that legitimately exercises Footlights services.
 * @author jon@footlights.me
 */
public class GoodPlugin extends me.footlights.core.plugin.Plugin
{
	public String name() { return "Good Plugin"; }
	public void setOutputStream(PrintWriter out) { this.out = out; }

	public void run()
	{
		try
		{
			out.println("I am a well-behaved plugin.");
			out.println("The time is " + new Date());

			out.print("Let's test a static method in the Helper class... ");
			out.println(Helper.staticHelp());

			out.println("Ok, that was fine. Now a constructor... ");
			Helper h = new Helper();

			out.print("And a regular method... ");
			out.println(h.help());

			out.print("Finally, do a 'syscall'... ");
			out.println("new UUID: " + kernel.generateUUID());

			out.println("The plugin works!.");
		}
		catch(Exception e) { throw new Error(e); }
	}

	/** Output stream */
	private PrintWriter out;
}

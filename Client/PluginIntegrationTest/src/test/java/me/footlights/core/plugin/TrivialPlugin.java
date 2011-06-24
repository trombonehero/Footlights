package me.footlights.core.plugin;

import java.io.PrintWriter;


class TrivialPlugin extends Plugin
{
	final static String OUTPUT = "Hello, world!";

	@Override public String name() { return "Trivial Plugin"; }
	@Override public void setOutputStream(PrintWriter out) { this.out = out; }

	public void run() { out.print(OUTPUT); }

	private PrintWriter out;
}

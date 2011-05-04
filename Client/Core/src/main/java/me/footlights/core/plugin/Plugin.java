package me.footlights.core.plugin;


import java.util.Date;


/** A social plugin */
public abstract class Plugin implements Runnable
{
	public Plugin() { loaded = new Date(); }

	public void setKernel(KernelInterface kernel) { this.kernel = kernel; }
	public Date loaded() { return loaded; }

	public abstract String name();
	public abstract void setOutputStream(java.io.PrintWriter out);


	/** When the plugin was loaded */
	private Date loaded;

	/** Access to the system "kernel" */
	protected KernelInterface kernel;
}

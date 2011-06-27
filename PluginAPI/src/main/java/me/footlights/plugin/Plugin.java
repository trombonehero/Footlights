package me.footlights.plugin;

import java.util.logging.Logger;


/** A Footlights plugin. */
public interface Plugin
{
	/** Start running. */
	public void run(KernelInterface kernel, Logger log) throws Exception;
}

package me.footlights.core.plugin;

import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;

import me.footlights.plugin.KernelInterface;
import me.footlights.plugin.Plugin;


class TrivialPlugin implements Plugin
{
	@Override public void run(KernelInterface kernel, Logger log)
	{
		log.info(OUTPUT);
	}

	@VisibleForTesting static String OUTPUT = "The trivial demo plugin is now running";
}

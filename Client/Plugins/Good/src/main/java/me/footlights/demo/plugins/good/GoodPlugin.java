package me.footlights.demo.plugins.good;

import java.util.Date;
import java.util.logging.Logger;

import me.footlights.plugin.KernelInterface;


/**
 * A well-behaved plugin that legitimately exercises Footlights services.
 * @author jon@footlights.me
 */
public class GoodPlugin implements me.footlights.plugin.Plugin
{
	@Override public void run(KernelInterface kernel, Logger log) throws Exception
	{
		log.info("I am a well-behaved plugin.");
		log.info("The time is " + new Date());

		log.info("Let's test a static method in the Helper class... ");
		log.info(Helper.staticHelp());

		log.info("Ok, that was fine. Now a constructor... ");
		Helper h = new Helper();

		log.info("And a regular method... ");
		log.info(h.help());

		log.info("Finally, do a 'syscall'... ");
		log.info("new UUID: " + kernel.generateUUID());

		log.info("The plugin works!.");
	}
}

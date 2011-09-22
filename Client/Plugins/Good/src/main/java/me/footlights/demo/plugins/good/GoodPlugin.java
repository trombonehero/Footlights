/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.footlights.demo.plugins.good;

import java.util.Date;
import java.util.logging.Logger;

import me.footlights.plugin.AjaxHandler;
import me.footlights.plugin.KernelInterface;


/**
 * A well-behaved plugin that legitimately exercises Footlights services.
 * @author jon@footlights.me
 */
public class GoodPlugin implements me.footlights.plugin.Plugin
{
	public static GoodPlugin init(KernelInterface kernel, Logger log) throws Exception
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

		return new GoodPlugin(kernel, new DemoAjaxHandler(), log);
	}

	@Override public AjaxHandler ajaxHandler() { return ajax; }

	private GoodPlugin(KernelInterface kernel, AjaxHandler ajax, Logger log)
	{
		this.ajax = ajax;
		this.kernel = kernel;
		this.log = log;
	}

	private final AjaxHandler ajax;
	private final KernelInterface kernel;
	private final Logger log;
}

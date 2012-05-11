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
package me.footlights.demos.good;

import scala.Option;
import java.util.logging.Logger;

import me.footlights.api.Directory;
import me.footlights.api.KernelInterface;
import me.footlights.api.ModifiablePreferences;
import me.footlights.api.ajax.AjaxHandler;
import me.footlights.api.ajax.JavaScript;


/**
 * A well-behaved application that legitimately exercises Footlights services.
 * @author jon@footlights.me
 */
public class GoodApp extends me.footlights.api.Application
{
	public static GoodApp init(KernelInterface kernel, ModifiablePreferences prefs, Logger log)
	{
		log.info("Loading " + GoodApp.class.getCanonicalName() + "...");
		return new GoodApp(new DemoAjaxHandler(kernel, log));
	}

	@Override public Option<AjaxHandler> ajaxHandler() { return Option.apply(ajax); }
	@Override public void open(Directory directory)
	{
		ajax.fireAsynchronousEvent(
			new JavaScript("context.root.appendElement('div').appendText('open(")
				.appendText(directory.toString())
				.append(")');")
		);
	}

	private GoodApp(AjaxHandler ajax)
	{
		super("Basic demo");
		this.ajax = ajax;
	}

	private final AjaxHandler ajax;
}

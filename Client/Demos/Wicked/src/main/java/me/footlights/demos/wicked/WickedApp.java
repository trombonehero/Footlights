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
package me.footlights.demos.wicked;

import java.util.logging.Logger;

import me.footlights.api.KernelInterface;
import me.footlights.api.ModifiablePreferences;
import me.footlights.api.ajax.AjaxHandler;


public class WickedApp implements me.footlights.api.Application
{
	public static WickedApp init(KernelInterface kernel, ModifiablePreferences prefs, Logger log)
	{
		return new WickedApp(new EvilAjaxHandler(kernel, log));
	}

	@Override public AjaxHandler ajaxHandler() { return ajaxHandler; }
	@Override public String shortName() { return "Wicked Demo"; }

	private WickedApp(AjaxHandler ajaxHandler) { this.ajaxHandler = ajaxHandler; }
	private final AjaxHandler ajaxHandler;
}

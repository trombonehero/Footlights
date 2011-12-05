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
package me.footlights.demo.plugins.wicked;

import java.util.logging.Logger;

import me.footlights.plugin.KernelInterface;
import me.footlights.plugin.ModifiablePreferences;
import me.footlights.plugin.ajax.AjaxHandler;


public class WickedPlugin implements me.footlights.plugin.Plugin
{
	public static WickedPlugin init(KernelInterface kernel, ModifiablePreferences prefs, Logger log)
	{
		return new WickedPlugin(new EvilAjaxHandler(kernel, log));
	}

	@Override public AjaxHandler ajaxHandler() { return ajaxHandler; }

	private WickedPlugin(AjaxHandler ajaxHandler) { this.ajaxHandler = ajaxHandler; }
	private final AjaxHandler ajaxHandler;
}

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
package me.footlights.plugin;


/**
 * A Footlights plugin.
 *
 * A plugin is initially loaded via an explicit URI + class name, e.g.
 * "jar:http://apps.foo.com/path/to/jarfile.jar!/com.foo.apps.somepackage.WhereItAllBegins".
 *
 * Footlights then looks for a method in WhereItAllBegins with the signature
 * <pre>static {@link Plugin} init({@link KernelInterface}, {@link java.util.Logger})</pre>.
 * This method is run, and is expected to return a fully-initialized {@link Plugin} object,
 * ready to respond to user input via its {@link AjaxHandler} (or other mechanisms, if we build
 * them in the future).
 */
public interface Plugin
{
	/**
	 * A handler for Ajax requests from the Web UI.
	 *
	 * @return null if there is no such handler
	 */
	public AjaxHandler ajaxHandler();
}

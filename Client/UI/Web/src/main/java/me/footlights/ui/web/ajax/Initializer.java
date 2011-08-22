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
package me.footlights.ui.web.ajax;

import me.footlights.ui.web.Request;

import static me.footlights.ui.web.ajax.JavaScript.ajax;


/** Initializes the UI after the static JavaScript has loaded. */
class Initializer implements AjaxHandler
{
	@Override public JavaScript service(Request request)
	{
		JavaScript script = new JavaScript();

		script.append("var buttons = document.getElementById('buttons');");
		script.append("buttons.innerHTML='';");

		script.append("var echo = sandboxes.create('echo', rootContext, 0, 0, 200, 200);");
		script.append("var foo = sandboxes.create('foo', rootContext, 0, 0, 200, 200);");
		script.append("foo.ajax('hello');");

		JavaScript fooAjax = new JavaScript();
		fooAjax.append("foo.ajax(" + HelloWorldPlugin.PATH + ");");

		script.append(button("Echo", ajax("echo stuff", "echo")));
		script.append(button("Foo", ajax("hello", "foo")));
		script.append(button("Cajole", ajax("cajole")));
		script.append(button("Good Plugin",
				ajax("load_plugin /good.jar!/me.footlights.demo.plugins.good.GoodPlugin")));
		script.append(button("Wicked Plugin",
				ajax("load_plugin /wicked.jar!/me.footlights.demo.plugins.wicked.WickedPlugin")));

		script.append(button("Reset", ajax("reset")));

		script.append("console.log('UI Initialized');");

		return script;
	}


	private static JavaScript button(String label, JavaScript onClick)
	{
		JavaScript script = new JavaScript();

		script.append("var button = document.createElement('button');");
		script.append("button.type = 'button';");

		script.append("button.appendChild(document.createTextNode('");
		script.append(label);
		script.append("'));");

		script.append("button.onclick = function() {");
		script.append(onClick);
		script.append("};");

		script.append("buttons.appendChild(button);");

		return script;
	}
}

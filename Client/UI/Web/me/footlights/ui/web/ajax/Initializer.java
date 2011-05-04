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

		script.append("var foo = sandboxes.create('foo', { appendChild: function(node) { context.root.appendChild(node); } }, 0, 0, 200, 200);");
/*		script.append("context.root.appendChild(foo.root);");
		script.append("foo.root.className = 'sandbox';");
		script.append("foo.root.appendChild(document.createTextNode('foo'));");
*/
		JavaScript fooAjax = new JavaScript();
		fooAjax.append("foo.ajax(" + HelloWorldPlugin.PATH + ");");

		script.append(button("Echo", ajax("echo stuff")));
		script.append(button("Foo", ajax(HelloWorldPlugin.PATH)));
		script.append(button("Cajole", ajax("cajole")));
		script.append(button("Good Plugin",
				ajax("load_plugin /good.jar!/me.footlights.demo.plugins.good.GoodPlugin")));
		script.append(button("Wicked Plugin",
				ajax("load_plugin /wicked.jar!/me.footlights.demo.plugins.wicked.WickedPlugin")));

		/*
		response.append("var sandbox = document.createElement('div');");
		response.append("sandbox.className = 'sandbox';");
		response.append("sandbox.width = 400;");
		response.append("document.getElementById('response').appendChild(sandbox);");
		response.append("retrieveAndRunModule('sandbox.js', 'sandboxed', sandbox, log);");
		response.append("};");
		*/

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

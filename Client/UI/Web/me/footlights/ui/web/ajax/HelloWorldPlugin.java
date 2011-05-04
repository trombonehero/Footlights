package me.footlights.ui.web.ajax;

import me.footlights.ui.web.Request;


/** A plugin that just prints "Hello, world!" within its sandbox. */
class HelloWorldPlugin implements AjaxHandler
{
	static final String PATH = "foo";

	@Override
	public JavaScript service(Request request) { return CODE; }

	private static final JavaScript CODE =
		new JavaScript()
			.append("context.root.appendElement('div')")
			.append(".appendText('Hello, world!')");
}

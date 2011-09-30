package me.footlights.demo.tictactoe;

import me.footlights.plugin.AjaxHandler;
import me.footlights.plugin.Context;
import me.footlights.plugin.JavaScript;
import me.footlights.plugin.WebRequest;


class AjaxContext extends Context
{
	AjaxContext()
	{
		register("init", new AjaxHandler()
		{	
			@Override
			public JavaScript service(WebRequest request)
			{
				return new JavaScript()
					.append("context.load('init.js');");
			}
			});

		register("clicked", new AjaxHandler()
			{
				@Override public JavaScript service(WebRequest request)
				{
					return new JavaScript()
						.append("context.log('clicked \\'")
						.appendText(request.path())
						.append("\\'');");
				}
			});
	}
}

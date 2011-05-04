package me.footlights.ui.web.ajax;

import me.footlights.ui.web.Request;


/** An object which can process Ajax requests. */
public interface AjaxHandler
{
	/** Service a request. */
	JavaScript service(Request request) throws Throwable;
}

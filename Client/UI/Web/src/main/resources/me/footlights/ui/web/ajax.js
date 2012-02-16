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
'use strict';

/**
 * Perform an Ajax call from within a particular execution context.
 *
 * @param callback     may be undefined, in which case a generic callback will be used
 *                     that requires the Ajax response to be executable JS code, and executes
 *                     it within the given context's sandbox
 */
function ajax(url, context, callback)
{
	var xhr = false;

	if(window.XMLHttpRequest) { xhr = new XMLHttpRequest(); }
	else if(window.ActiveXObject) { xhr = new ActiveXObject("Microsoft.XMLHTTP"); }

	try
	{
		xhr.open('GET', url);
		xhr.onreadystatechange =
			function ajaxStateChange() { forwardAjaxResponse(xhr, url, context, callback); };

		xhr.open('GET', 'http://localhost:4567/' + url, true);
		xhr.send(null);
	}
	catch(e) { context.log('Error performing Ajax call: "' + e + '"'); }
}


/** Forwards Ajax responses to the correct execution context. */
function forwardAjaxResponse(xhr, request, context, callback)
{
	if(xhr.readyState != 4) return;
	if(xhr.status != 200)
	{
		context.log('Error serving Ajax request "' + request + '": HTTP status ' + xhr.status);
		return;
	}

	// Parse the Ajax response.
	var contentType = xhr.getResponseHeader('Content-Type');
	var response = null;
	switch (contentType)
	{
		case 'application/json':     response = JSON.parse(xhr.responseText); break;
		case 'text/javascript':      response = xhr.responseText; break;
		case 'text/xml':             response = xhr.responseXML; break;
	}

	// If no callback has been specified, the response had better be code to be executed.
	if (callback == undefined)
		if (contentType == 'text/javascript')
			callback = function defaultAjaxCallback(code) { context.exec(code); }
		else throw 'No callback given for Ajax response of type "' + contentType + '"';

	callback(response);
}

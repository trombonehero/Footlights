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

function ajax(url, context, callback)
{
	var xhr = false;

	if(window.XMLHttpRequest) { xhr = new XMLHttpRequest(); }
	else if(window.ActiveXObject) { xhr = new ActiveXObject("Microsoft.XMLHTTP"); }

	try
	{
		xhr.open('GET', url);
		xhr.onreadystatechange = function() { forwardAjaxResponse(xhr, url, context, callback); };

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

	// The response might be an XML object or just plain text.
	var contentType = xhr.getResponseHeader('Content-Type');
	var response = (contentType == 'text/xml') ? xhr.responseXML : xhr.responseText;

	// If no callback has been specified, just assume the response is code to be executed.
	if (callback == undefined) callback = function(code) { context.exec(code); }
	callback(response);
}

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

function ajax(url, context)
{
	var xhr = false;

	if(window.XMLHttpRequest) { xhr = new XMLHttpRequest(); }
	else if(window.ActiveXObject) { xhr = new ActiveXObject("Microsoft.XMLHTTP"); }

	try
	{
		xhr.open('GET', url);
		xhr.onreadystatechange = function() { handleAjax(xhr, url, context); };

		xhr.open('GET', 'http://localhost:4567/' + url, true);
		xhr.send(null);
	}
	catch(e) { context.log('Error performing Ajax call: "' + e + '"'); }
}


function handleAjax(xhr, request, context)
{
	if(xhr.readyState == 4)
	{
		if(xhr.status != 200)
		{
			context.log('Error serving Ajax request "' + request + '": HTTP status ' + xhr.status);
			return;
		}

		switch (xhr.getResponseHeader('Content-Type'))
		{
			case 'text/javascript':
				context.exec(xhr.responseText);
				return;

			default:
				context.log('unknown XHR response type: ' + xhr.getResponseHeader('Content-Type'));
				return;
		}
	}
}

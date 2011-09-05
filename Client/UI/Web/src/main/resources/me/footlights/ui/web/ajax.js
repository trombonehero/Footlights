var xhr = false;

if(window.XMLHttpRequest) { xhr = new XMLHttpRequest(); } 
else if(window.ActiveXObject) { xhr = new ActiveXObject("Microsoft.XMLHTTP"); }


function ajax(request, context) { ajaxWithCallback(request, context, defaultCallback); }
function ajaxWithCallback(url, context, callback)
{
	try
	{
		xhr.open('GET', url);
		xhr.onreadystatechange = function() { handleAjax(xhr, url, context, callback); };

		xhr.open('GET', 'http://localhost:4567/' + url, true);
		xhr.send(null);
	}
	catch(e) { updateStatus(e); }
}


function handleAjax(xhr, request, context, callback)
{
	if(xhr.readyState == 4)
	{
		if(xhr.status != 200) return;

		switch (xhr.getResponseHeader('Content-Type'))
		{
			case 'text/javascript':
				context.exec(xhr.responseText);
				return;

			case 'text/xml':
				var doc = xhr.responseXML.documentElement;
				var type = doc.getElementsByTagName('type')[0].childNodes[0].nodeValue;
				var content = doc.getElementsByTagName('content')[0].childNodes[0].nodeValue;

				if(type == "code") callback(context, content);
				else if(type == "error") showError(context, content);
				else showAjaxResponse(type, content);

				return;

			default:
				context.log('unknown XHR response type: ' + xhr.getResponseHeader('Content-Type'));
				return;
		}
	}
}

function defaultCallback(context, content)
{
	context.log('got code for context "' + context.name + '"');
	context.exec(content);
}

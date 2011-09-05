var xhr = false;

if(window.XMLHttpRequest) { xhr = new XMLHttpRequest(); } 
else if(window.ActiveXObject) { xhr = new ActiveXObject("Microsoft.XMLHTTP"); }


function ajax(url, context)
{
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

			case 'text/xml':
				var doc = xhr.responseXML.documentElement;
				var type = doc.getElementsByTagName('type')[0].childNodes[0].nodeValue;
				var content = doc.getElementsByTagName('content')[0].childNodes[0].nodeValue;

				if(type == "error") showError(context, content);
				else showAjaxResponse(type, content);

				return;

			default:
				context.log('unknown XHR response type: ' + xhr.getResponseHeader('Content-Type'));
				return;
		}
	}
}

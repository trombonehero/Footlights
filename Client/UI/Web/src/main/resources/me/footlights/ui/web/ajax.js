var xhr = false;

if(window.XMLHttpRequest) { xhr = new XMLHttpRequest(); } 
else if(window.ActiveXObject) { xhr = new ActiveXObject("Microsoft.XMLHTTP"); }



function ajax(request, context) { ajaxWithCallback(request, context, ajaxCallback); }
function ajaxWithCallback(request, context, callback)
{
	try
	{
		xhr.onreadystatechange = function() { callback(request, context); };

		console.log('sending request ' + request + ' to context ' + context.name);
		request = context.name + '/' + request;
		console.log(request);

		xhr.open('GET', 'http://localhost:4567/' + request, true);
		xhr.send(null);
	}
	catch(e) { updateStatus(e); }
}


function ajaxCallback(request, context)
{
	if(xhr.readyState == 4)
	{
		if(xhr.status != 200) return;

		var xml = xhr.responseXML;
		if(xml == null)
		{
			console.log('NULL responseXML (request: "' + request + '"');
			return;
		}

		var xmldoc = xml.documentElement;
		var type = xmldoc.getElementsByTagName('type')[0].childNodes[0].nodeValue;
		var content = xmldoc.getElementsByTagName('content')[0].childNodes[0].nodeValue;
//		var context = xmldoc.getElementsByTagName('context')[0].childNodes[0].nodeValue;

		if(type == "error") showError(context, content);
		else if(type == "code")
		{
			console.log('code for "' + context.name + '": ' + content);
			context.exec(content);
			/*
			if(context == 'global')
			{
				eval(content);
			}
			else
			{
				var sandbox = sandboxes[context];
				if (typeof(sandbox) == 'undefined')
				{
					sandbox = sandboxes.create(context, 250, 50, 400, 400);
				}

				runModule(content, context, sandbox);
			}
			*/
		}
		else showAjaxResponse(type, content);
	}
}

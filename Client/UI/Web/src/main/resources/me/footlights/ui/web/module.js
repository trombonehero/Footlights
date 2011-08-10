function runModule(code, name, root, log)
{
	var module = cajaVM.compileModule(code);

	var ret = module(
		{
			context:
			{
				ajax: function(request) { ajax(request, this, log); },
				name: name,
				root: proxy(root)
			},
			log: log,
		});
}


function retrieveAndRunModule(url, name, root, log)
{
	// Retrieve the sandbox code.
	var req = new XMLHttpRequest();
	req.open('GET', url);
	req.onreadystatechange = function statechange()
	{
		switch (this.readyState)
		{
			case XMLHttpRequest.DONE:
			case 4:  // Firefox 4 doesn't know that DONE==4!?
				try { runModule(this.responseText, name, root, log); }
				catch(e)
				{
					log.log('Error compiling ' + url + ': ' + e);
					log.log(e.stack);
				}
				break;
		}
	};
	req.send(null);
}

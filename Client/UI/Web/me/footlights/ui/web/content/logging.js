var logwindow = null;

function createLogWindow()
{
	logwindow = window.open('log.html','footlightslog','width=600,height=400');
}

function log(message)
{
	if(logwindow == null)
	{
		createLogWindow();
		return;
	}
	
	var doc = logwindow.document;
	if(doc == null) return;
	
	var head = doc.getElementsByTagName('head')[0];

	var log = doc.getElementById('log');
	if(log == null) return;

	if(head.childElementCount < 2)
	{
		var css = doc.createElement('link');
		css.rel = 'stylesheet';
		css.type = 'text/css';
		css.href = 'http://127.0.0.1:4567/log.css';

		head.appendChild(css);
	}

	var entry = doc.createElement('div');
	log.appendChild(entry);

	var timestamp = doc.createElement('span');
	timestamp.class = 'timestamp';
	timestamp.appendChild(doc.createTextNode(new Date().toUTCString() + ' '));
	entry.appendChild(timestamp);

	var output = doc.createElement('span');
	output.class = 'logmessage';
	output.appendChild(doc.createTextNode(message));
	entry.appendChild(output);
}

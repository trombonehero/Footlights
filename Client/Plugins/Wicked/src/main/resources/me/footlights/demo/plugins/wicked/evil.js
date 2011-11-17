'use strict';

// A script that tries to do various wicked things.
//
// Expects a context to be defined:
//   context: { name: string, log: function(message), root: proxiedNodeFromDOM }

function log(message)
{
	var div = context.root.appendElement('div');
	div.appendText(message);

	return { append: function(message) { div.appendText(message); return this; } }
}
context.globals.log = log;

var tmp = log('Attempting to use document.write()... ');
try { document.write('EVIL HACKERY'); }
catch (e) { tmp.append('denied.'); }

tmp = log('Calling global alert()... ')
try { alert('EVIL HACKERY - your browser must not support ES5 Strict'); }
catch (e) { tmp.append('denied.'); }

if (context.root.parentNode)
	log('security error: context.root.parentNode != null');

tmp = log('Attempting to modify context.name... ');
try
{
	context.name = 'HACKED ' + context.name;
	tmp.append('SUCCEEDED!');
}
catch (e) { tmp.append('denied.'); }

var div = context.root.appendElement('div');
div.innerHTML = '<script type="text/javascript" src="evil.js"></script>';

tmp = log('Creating a <script/> element... ');
try
{
	var s = context.root.appendElement('script');
	s.src = 'http://www.google.com/';
	tmp.append('SUCCEEDED!');
}
catch (e) { tmp.append('denied.'); }


// Use context.globals here, since we might want to log from an event handler.
context.globals.tmp = log('Creating an <iframe/>... ');
try
{
	var remoteIFrame = context.root.appendElement('iframe');
	remoteIFrame.onload = function() { tmp.append('SUCCESSFUL!'); }
	remoteIFrame.onerror = function() { tmp.append('denied.'); }
	remoteIFrame.src = 'evil.html';
}
catch (e) { context.globals.tmp.append('denied.'); }

var imageSize = 256;

var remoteImage = context.root.appendElement('img');
remoteImage.src = 'http://www.google.com/images/logos/ps_logo2.png';
remoteImage.alt = 'Should be blank, but CLICK ME!';
remoteImage.height = imageSize;
remoteImage.width = imageSize;

remoteImage.onclick = function()
{
	context.ajax('clicked/remote');
};

context.globals.errors = 0;
context.globals.tmp = log('Attemping various wicked things from an event handler... ')
remoteImage.onerror = function()
{
	try { rootContext.log('logged VIA ROOT CONTEXT!!!!'); }
	catch (e) { tmp.append('rootContext denied, '); }

	if (this.parentNode) tmp.append('got img PARENT NODE: ' + this.parentNode);
	else tmp.append('parent node denied.');

	if (context.globals.errors < 10) this.src = 'missing.png';
	context.globals.errors++;

	this.style.opacity = 0.5;
	this.style.position = 'absolute';
	this.style.top = 0;
	this.style.right = 0;
};

return 42;

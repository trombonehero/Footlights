'use strict';

// A script that tries to do various wicked things.
//
// Expects a context to be defined:
//   context: { name: string, log: function(message), root: proxiedNodeFromDOM }


var foo = 'foo';

try { document.write('EVIL HACKERY'); }
catch (e) { context.log('document.write() not allowed (good!)'); }

try { alert('EVIL HACKERY - your browser must not support ES5 Strict'); }
catch (e) { context.log('alert() not allowed (good!)'); }

if (context.root.parentNode)
	context.log('security error: context.root.parentNode != null');

try { context.name = 'HACKED ' + context.name; }
catch (e) { context.log('modifying context.name not allowed (good!)'); }

var text = context.name + ' - brought to you by sandboxed JavaScript code';
var p = context.root.appendElement('p');
var t = p.appendText(text);

var div = context.root.appendElement('div');
div.innerHTML = '<script type="text/javascript" src="evil.js"></script>';

try
{
	var s = context.root.appendElement('script');
	s.src = 'http://www.google.com/';
	context.log('Script created a <script/> element (bad!)');
}
catch (e) { context.log('Creating <script/> element not allowed (good!)'); }

var imageSize = 256;
var previousSize = imageSize;
var nextSize = 32;

var localImage = context.root.appendElement('img');
localImage.onmouseout = function()
{
	this.style.opacity = 1;
	p.style.color = 'black';
};
localImage.onmouseover = function()
{
	this.style.opacity = 0.5;
	p.style.color = 'blue';
};
localImage.src = 'images/local.png';
localImage.width = imageSize;

try
{
	var remoteImage = context.root.appendElement('img');
	remoteImage.src = 'http://www.google.com/images/logos/ps_logo2.png';
	remoteImage.height = imageSize;
	remoteImage.width = imageSize;
}
catch (e) { context.log('Failed to load remote image (good!)'); }

var remoteImage = context.root.appendElement('img');
remoteImage.src = 'www.google.com/images/logos/ps_logo2.png';
remoteImage.alt = 'Should be blank, but CLICK ME!';
remoteImage.height = imageSize;
remoteImage.width = imageSize;

remoteImage.onclick = function()
	{
		context.ajax('Clicked the remote image');
	};

localImage.onclick = function()
	{
		remoteImage.height = remoteImage.width = nextSize;
		var tmp = previousSize;
		previousSize = nextSize;
		nextSize = tmp;
		context.ajax('Clicked the local image');
	};

return 42;

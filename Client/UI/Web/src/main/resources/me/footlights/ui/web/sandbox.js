'use strict';

// A script that tries to do various wicked things.
//
// Expects two variables to be defined:
//   context: { name: string, root: proxiedNodeFromDOM }
//   log: { log: function(messageToBeLogged) }


var foo = 'foo';

try { document.write('EVIL HACKERY'); }
catch (e) { log.log('document.write() not allowed (good!)'); }

try { alert('EVIL HACKERY - your browser must not support ES5 Strict'); }
catch (e) { log.log('alert() not allowed (good!)'); }

if (context.root.parentNode)
	log.log('security error: context.root.parentNode != null');

log.log('context.root: ' + JSON.stringify(context.root));

var text = context.name + ' - brought to you by sandboxed JavaScript code';
var p = context.root.appendElement('b');
var t = p.appendText(text);

var div = context.root.appendElement('div');
div.innerHTML = '<script type="text/javascript" src="evil.js"></script>';

try
{
	var s = context.root.appendElement('script');
	s.src = 'http://www.google.com/';
	log.log('Script created a <script/> element (bad!)');
}
catch (e) { log.log('Creating <script/> element not allowed (good!)'); }

var imageSize = 256;
var previousSize = imageSize;
var nextSize = 32;

var localImage = context.root.appendElement('img');
localImage.onmouseout = function() { p.style.color = 'black'; };
localImage.onmouseover = function() { p.style.color = 'blue'; };
localImage.src = '/images/local.jpeg';
localImage.width = imageSize;

try
{
	var remoteImage = context.root.appendElement('img');
	remoteImage.src = 'http://www.google.com/images/logos/ps_logo2.png';
	remoteImage.height = imageSize;
	remoteImage.width = imageSize;
}
catch (e) { log.log('Failed to load remote image (good!)'); }

var remoteImage = context.root.appendElement('img');
remoteImage.src = 'www.google.com/images/logos/ps_logo2.png';
remoteImage.alt = 'Should be blank';
remoteImage.height = imageSize;
remoteImage.width = imageSize;

localImage.onclick = function()
	{
		remoteImage.height = remoteImage.width = nextSize;
		var tmp = previousSize;
		previousSize = nextSize;
		nextSize = tmp;
	};

return 42;
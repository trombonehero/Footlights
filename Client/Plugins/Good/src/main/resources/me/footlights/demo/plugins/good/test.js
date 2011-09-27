'use strict';

// A script that behaves well.
//
// Expects a context to be defined:
//   context: { name: string, log: function(message), root: proxiedNodeFromDOM }


var foo = 'foo';

var text = context.name + ' - brought to you by sandboxed JavaScript code';
var p = context.root.appendElement('p');
var t = p.appendText(text);

var imageSize = 256;
var previousSize = imageSize;
var nextSize = 32;

var localImage = context.root.appendElement('img');
localImage.id = 'sandboxedimage';
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
localImage.onclick = function()
{
	this.style.opacity = 0.1;
	p.style.color = 'green';
};
localImage.src = 'images/local.png';
localImage.width = imageSize;

return 42;

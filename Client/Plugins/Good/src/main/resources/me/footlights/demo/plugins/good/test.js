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

var localImage = context.root.appendElement('img');
localImage.id = 'sandboxedimage';
localImage.height = imageSize;
localImage.width = imageSize;
localImage.onmouseout = function()
{
	this.style.opacity = 0.5;
	p.style.color = 'blue';
};
localImage.onmouseover = function()
{
	this.style.opacity = 1;
	p.style.color = 'green';
};
localImage.onclick = function()
{
	context.ajax('clicked/local');
	this.style.opacity = 0.1;
	p.style.color = 'red';
};
localImage.onload = function()
{
	context.log('image loaded');

	p.style.color = 'blue';

	this.style.opacity = 0.5;
	this.style.position = 'absolute';
	this.style.top = 0;
	this.style.right = 0;
};

context.log('loading local image...');
localImage.src = 'images/local.png';

return 42;

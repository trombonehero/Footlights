'use strict';

// A script that behaves well.
//
// Expects a context to be defined:
//   context: { name: string, log: function(message), root: proxiedNodeFromDOM }


var foo = 'foo';

var text = context.name + ' - brought to you by sandboxed JavaScript code';
var p = context.root.appendElement('p');
p.appendText(text);
context.globals.p = p;

var hello = context.root.appendElement('p');
hello.appendText('Hello, ');
hello.appendPlaceholder('user.name');
hello.appendText('!');

var imageSize = 256;

var localImage = context.root.appendElement('img');

localImage.class = 'sandboxedimage';
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
	this.style.opacity -= 0.1;
	p.style.color = 'red';
};

var imgload_text = context.root.appendElement('p');
imgload_text.appendText('loading local image... ');
context.globals.t = imgload_text;

localImage.onload = function()
{
	t.appendText('image loaded.');

	p.style.color = 'blue';

	this.style.opacity = 0.5;
	this.style.position = 'absolute';
	this.style.top = 0;
	this.style.right = 0;
};

localImage.onerror = function()
{
	t.appendText('error!');
	p.style.color = 'orange';
}

localImage.src = 'images/local.png';

var b = context.root.appendElement('input');
b.type = 'button';
b.value = 'Open a File';
b.onclick = function() { context.ajax('open_file'); }

context.ajax('log', function(response) { context.log(response); });

return 42;

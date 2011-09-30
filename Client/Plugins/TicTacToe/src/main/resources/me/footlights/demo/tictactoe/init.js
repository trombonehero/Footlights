function create_space(x, y)
{
	var box = context.root.appendElement('img');
	box.x = x;
	box.y = y;

	box.onload = function()
	{
		this.style.background = 'white';
		this.style.opacity = 0.5;

		this.style.height = 94;
		this.style.width = 94;

		this.style.position = 'absolute';
		this.style.left = 94 * this.x;
		this.style.top = 94 * this.y;
	};
	box.src = 'square.png';
	box.onclick = function() { context.ajax('clicked/' + this.x + ',' + this.y); };
}

for (var i = 0; i < 3; i++)
	for (var j = 0; j < 3; j++)
		create_space(i, j);


var next_box = context.root.appendElement('div');
next_box.style.position = 'relative';
next_box.style.left = 350;

next_box.style.background = 'black';
next_box.style.opacity = 0.5;
next_box.style.width = 94;
next_box.style.height = 94;

var next = next_box.appendElement('img');
context.globals.next = next;

next.onload = function()
{
	this.style.position = 'relative';
	this.style.top = -32;
	this.style.left = -35;
}

next.src = 'cross.png';


function place(filename, x, y, offset)
{
	var img = context.root.appendElement('img');
	img.x = 94 * x - offset - 4;
	img.y = 94 * y - offset;

	img.onload = function()
	{
		this.style.position = 'absolute';
		this.style.left = this.x;
		this.style.top = this.y;
	};
	img.src = filename;
}

context.globals.place = place;

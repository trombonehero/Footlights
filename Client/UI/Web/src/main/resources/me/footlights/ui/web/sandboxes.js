'use strict';

// Illegal sandbox names: 'create' and 'global'.
var sandboxes = {};

sandboxes.getOrCreate = function(name, parent, log, x, y, width, height)
{
	if (name in sandboxes) return sandboxes[name]
	else return sandboxes.create(name, parent, log, x, y, width, height)
}

sandboxes.create = function(name, parent, log, x, y, width, height)
{
	var container = document.createElement('div');

	var label = document.createElement('div');
	container.appendChild(label);
	label.className = 'sandboxlabel';
	label.innerHTML = 'Sandbox: ' + name;

	var content = document.createElement('div');
	container.appendChild(content);

	content.className = 'sandbox';
	content.style.background = '#ffc';
	// TODO: (x,y)
	content.width = width;
	content.height = height;

	parent.root.appendChild(container);

	var sandbox =
		{
			ajax: function(request) { ajax('/ajax/' + name + '/' + request, this); },
			exec: function(request) { cajaVM.compileModule(request)({ 'context': this }); },
			load: function(filename) { ajax('static/' + this.name + '/' + filename, this); },
			log: log,
			name: name,
			root: proxy(content, name),
		};

	sandboxes[name] = sandbox;		
	return sandbox;
};

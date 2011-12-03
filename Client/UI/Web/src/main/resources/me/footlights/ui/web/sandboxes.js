/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
			compile: function(code) { return cajaVM.compileModule('return ' + code); },
			exec: function(code) { cajaVM.compileModule(code)({ 'context': this }); },
			load: function(filename) { ajax('static/' + this.name + '/' + filename, this); },
			log: log,
			name: name,
		};

	// Define a place to put global variables, which are automatically added to
	// the scope of proxied code like event handlers. We define one such
	// variable to be 'context', a reference to our sandbox.
	//
	// This global-variable-to-auto-scoped-variable facility supports proxied
	// DOM event handlers, allowing code such as:
	//
	// var foo = { run: function() { ... } };
	// context.globals['custom_name_for_foo'] = foo;
	//
	// an_image.onload = function() { custom_name_for_foo.run(); }
	sandbox.globals = { context: sandbox };
	sandbox.root = proxy(content, sandbox);
	sandbox = Object.freeze(sandbox);

	sandboxes[name] = sandbox;		
	return sandbox;
};

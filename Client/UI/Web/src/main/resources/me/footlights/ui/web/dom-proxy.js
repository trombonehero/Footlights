// Proxies access to DOM nodes.
//
// Provides a limited number of safe [TODO: are they?] DOM methods:
//   appendText(text)              returns a proxied Text node
//   appendElement(type)           returns a proxied Node
//

placeholders = {}
max_placeholder_id = 0

function proxy(node, context)
{
	var theProxy =
	{
		clear: function()
		{
			while (node.childNodes.length >= 1)
				node.removeChild(node.firstChild);
		},

		appendText: function(text)
		{
			var element = document.createTextNode(text);
			node.appendChild(element);
			return proxy(element, context);
		},

		appendElement: function(type)
		{
			var element = null;
			var subproxy = null;

			switch (type)
			{
				case 'iframe':
				case 'script':
					throw 'Sandboxed script attempted to create a ' + type + ' element';

				default:
					element = document.createElement(type);
					subproxy = proxy(element, context);
			}

			node.appendChild(element);
			return subproxy;
		},

		appendPlaceholder: function(name)
		{
			// We must create the span explicitly, rather than using appendElement(),
			// since we want to set the 'id' attribute (which is a privileged operation).
			var span = document.createElement('span');
			node.appendChild(span);

			// Set a unique ID so that we can come along later and fill in the placeholder.
			max_placeholder_id += 1
			var id = max_placeholder_id
			span.setAttribute('id', 'placeholder_' + id);

			// Now we can start using the unprivileged proxy for the <span/>.
			var subproxy = proxy(span, context);
			subproxy.class = 'placeholder';
			subproxy.appendText('${' + name + '}');
			return subproxy;
		},

		get style() { return node.style; },

		set src(uri)        { node.src = '/static/' + context.name + '/' + uri; },

		set alt(text)       { node.alt = text; },
		set class(name)     { node.setAttribute("class", name); },
		set type(t)         { node.type = t; },
		set value(v)        { node.value = v; },
		set height(x)       { node.height = x; },
		set width(x)        { node.width = x; },


		// Event handlers must be proxied so that, when called, 'this' refers to the proxy object
		// and not the naked DOM object.
		proxy_code: function(js)
		{
			theProxy[js] = context.compile(js)(context.globals);
			return function() { theProxy[js](); }
		},

		set onclick(js)     { node.onclick      = theProxy.proxy_code(js); },
		set onerror(js)     { node.onerror      = theProxy.proxy_code(js); },
		set onload(js)      { node.onload       = theProxy.proxy_code(js); },
		set onmouseout(js)  { node.onmouseout   = theProxy.proxy_code(js); },
		set onmouseover(js) { node.onmouseover  = theProxy.proxy_code(js); },
	};

	return theProxy;
}


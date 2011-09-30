// Proxies access to DOM nodes.
//
// Provides a limited number of safe [TODO: are they?] DOM methods:
//   appendText(text)              returns a proxied Text node
//   appendElement(type)           returns a proxied Node
//
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
				case 'script':
					throw 'Sandboxed script attempted to create a ' + type + ' element';

				default:
					element = document.createElement(type);
					subproxy = proxy(element, context);
			}

			node.appendChild(element);
			return subproxy;
		},

		get style() { return node.style; },

		set src(uri)        { node.src = '/static/' + context.name + '/' + uri; },

		set alt(text)       { node.alt = text; },
		set class(name)     { node.class = name; },
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


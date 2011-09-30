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
					throw 'Sandboxed script attempted to create a ' +
						type + ' element';

				// Allow only local images to be loaded.
				case 'img':
					element = document.createElement('img');
					subproxy =
					{
						set src(uri)
						{
							if (uri.indexOf(':') != -1)
								throw 'Sandboxed script attempted to load' +
									' an image with an absolute URI';

							else element.src = '/static/' + context.name + '/' + uri;
						},

						get style() { return element.style; },

						set alt(text) { element.alt = text; },
						set class(name) { element.class = name; },
						set height(x) { element.height = x; },
						set width(x) { element.width = x; },

						// Proxy a method so that, when called, 'this' is defined to
						// be the proxy, rather than the naked DOM object.
						proxy_code: function(js)
						{
							subproxy[js] = context.compile(js)(context.globals);
							return function() { subproxy[js](); }
						},

						set onclick(js)     { element.onclick = this.proxy_code(js); },
						set onerror(js)     { element.onerror = this.proxy_code(js); },
						set onload(js)      { element.onload = this.proxy_code(js); },
						set onmouseout(js)  { element.onmouseout = this.proxy_code(js); },
						set onmouseover(js) { element.onmouseover = this.proxy_code(js); },
					};
					break;

				default:
					element = document.createElement(type);
					subproxy = proxy(element, context);
			}

			node.appendChild(element);
			return subproxy;
		},

		get style() { return node.style; },
	};

	return theProxy;
}


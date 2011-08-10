// Proxies access to DOM nodes.
//
// Provides a limited number of safe [TODO: are they?] DOM methods:
//   appendText(text)              returns a proxied Text node
//   appendElement(type)           returns a proxied Node
//
function proxy(node)
{
	var theProxy =
	{
		appendText: function(text)
		{
			var element = document.createTextNode(text);
			node.appendChild(element);
			return proxy(element);
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

							else element.src = uri;
						},

						set alt(text) { element.alt = text; },
						set height(x) { element.height = x; },
						set onclick(js) { element.onclick = js; },
						set onmouseout(js) { element.onmouseout = js; },
						set onmouseover(js) { element.onmouseover = js; },
						set width(x) { element.width = x; },
					};
					break;

				default:
					element = document.createElement(type);
					subproxy = proxy(element);
			}

			node.appendChild(element);
			return subproxy;
		},

		get style() { return node.style; },
	};

	return theProxy;
}


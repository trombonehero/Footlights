/* Initialise Footlights */
var head = document.getElementsByTagName('head')[0];
var body = document.getElementsByTagName('body')[0];


/* Create an iframe for the server-generated parts */
var iframe = document.createElement('iframe');
iframe.src = 'http://localhost:4567/';
iframe.width = '100%';
iframe.height = '95%';


/* Remove everything under 'root' and show the iframe */
while (body.hasChildNodes()) body.removeChild(body.firstChild);
body.appendChild(iframe);


/* Load a fresh style sheet */
var css = document.createElement('link');
css.rel = 'stylesheet';
css.type = 'text/css';
css.href = '/style.css';
head.appendChild(css);

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

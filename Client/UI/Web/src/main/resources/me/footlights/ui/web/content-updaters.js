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

function insertContent(content)
{
	var contentDiv = document.createElement('div');
	contentDiv.className = 'content';
	contentDiv.appendChild(content);

	var contentRoot = document.getElementById('content');
	contentRoot.insertBefore(contentDiv, contentRoot.childNodes[3]);
}


function showAjaxResponse(type, content)
{
	var newcontent = document.createElement('div');
	newcontent.className = 'response';

	var typeDiv = document.createElement('div');
	typeDiv.className = 'type';
	typeDiv.appendChild(document.createTextNode(type));
	newcontent.appendChild(typeDiv);

	var contentDiv = document.createElement('div');
	contentDiv.className = 'content';
	contentDiv.appendChild(document.createTextNode(content));
	newcontent.appendChild(contentDiv);

	insertContent(newcontent);
}


function showError(context, err)
{
	var newcontent = document.createElement('div');
	newcontent.className = 'error';

	var contextDiv = document.createElement('div');
	contextDiv.className = 'ajaxContext';
	contextDiv.appendChild(document.createTextNode(context));
	newcontent.appendChild(contextDiv);

	var contentDiv = document.createElement('div');
	contentDiv.className = 'message';
	contentDiv.appendChild(document.createTextNode(err));
	newcontent.appendChild(contentDiv);

	insertContent(newcontent);
}

/*
 * Copyright 2012 Jonathan Anderson
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

var table = context.root.appendElement('table');
var current_row = table.appendElement('tr');
var rowlen = 0;

var toolbar = context.root.appendElement('div');
context.globals['toolbar'] = toolbar;

context.globals['clear'] = function clear()
{
	toolbar.clear();
	table.clear();
	current_row = table.appendElement('tr');
	rowlen = 0;
};

context.globals['new_album'] = function new_album(name, cover,
		openCallback, shareCallback, deleteCallback)
{
	if (rowlen > 3) {
		current_row = table.appendElement('tr');
		rowlen = 0;
	}

	var cell = current_row.appendElement('td');
	cell.class = 'photo';
	cell.style.border = '1px solid #999';
	cell.style.height = '198px';
	cell.style.width = '154px';
	cell.style['text-align'] = 'center';
	cell.style['vertical-align'] = 'top';

	var a = cell.appendElement('a');
	a.style.height = '20px';
	a.appendText(name);
	a.name = name;
	a.onclick = openCallback;

	var container = cell.appendElement('div');
	container.style.border = '1px solid #ccc';
	container.style['text-align'] = 'center';
	container.style.height = '150px';
	container.style.width = '150px';
	container.style['vertical-align'] = 'middle';

	var buttons = cell.appendElement('div');
	buttons.style.position = 'relative';

	var open = buttons.appendElement('img');
	open.src = 'images/oxygen/actions/system-run.png';
	open.style.position = 'absolute';
	open.style.height = 24;
	open.style.right = 24;
	open.name = name;
	open.onclick = openCallback;

	var share = buttons.appendElement('img');
	share.src = 'images/oxygen/actions/user-group-new.png';
	share.style.position = 'absolute';
	share.style.height = 24;
	share.style.right = 26;
	share.name = name;
	share.onclick = shareCallback;

	var del = buttons.appendElement('img');
	del.src = 'images/oxygen/actions/edit-delete.png';
	del.style.position = 'absolute';
	del.style.height = 24;
	del.style.right = '0';
	del.name = name;
	del.onclick = deleteCallback;

	var i = container.appendElement('img');
	i.src = cover;
	i.style['max-height'] = '150px';
	i.style['max-width'] = '150px';
	i.style['vertical-align'] = 'middle';
	i.name = name;
	i.onclick = openCallback;

	rowlen += 1;
}

context.globals['new_photo'] = function new_photo(name, deleteCallback)
{
	if (rowlen > 3) {
		current_row = table.appendElement('tr');
		rowlen = 0;
	}

	var cell = current_row.appendElement('td');
	cell.class = 'photo';
	cell.style.border = '1px solid #999';
	cell.style.height = '178px';
	cell.style.width = '154px';
	cell.style['text-align'] = 'center';
	cell.style['vertical-align'] = 'top';

	var container = cell.appendElement('div');
	container.style.border = '1px solid #ccc';
	container.style['text-align'] = 'center';
	container.style.height = '150px';
	container.style.width = '150px';
	container.style['vertical-align'] = 'middle';

	var buttons = cell.appendElement('div');
	buttons.style.position = 'relative';

	var del = buttons.appendElement('img');
	del.src = 'images/oxygen/actions/edit-delete.png';
	del.style.position = 'absolute';
	del.style.height = 24;
	del.style.right = '0';
	del.name = name;
	del.onclick = deleteCallback;

	var i = container.appendElement('img');
	i.filename = name;
	i.style['max-height'] = '150px';
	i.style['max-width'] = '150px';
	i.style['vertical-align'] = 'middle';

	rowlen += 1;
}

var status = context.root.appendElement('div');
context.globals['status'] = status;

context.ajax('populate');

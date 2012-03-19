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
var a = context.root.appendElement('a');
a.onclick = function() { context.ajax('do_upload'); }
a.appendText('Upload new photo');

var table = context.root.appendElement('table');
var current_row = table.appendElement('tr');
var rowlen = 0;

context.globals['clear'] = function clear()
{
	table.clear();
	current_row = table.appendElement('tr');
	rowlen = 0;
};

context.globals['new_photo'] = function new_photo(name)
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
	del.src = 'images/delete.png';
	del.style.position = 'absolute';
	del.style.right = '0';
	del.name = name;
	del.onclick = function remove_image() { context.ajax('remove/' + this.name); };

	var i = container.appendElement('img');
	i.src = name;
	i.style['max-height'] = '150px';
	i.style['max-width'] = '150px';
	i.style['vertical-align'] = 'middle';

	rowlen += 1;
}

var status = context.root.appendElement('div');
context.globals['status'] = status;

context.ajax('populate');

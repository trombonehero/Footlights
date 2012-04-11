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
var status = context.root.appendElement('div');
context.globals['status'] = status;

var breadcrumbs = context.root.appendElement('div');
breadcrumbs.style['background-color'] = 'rgba(153, 153, 153, 0.3)';
context.globals['breadcrumbs'] = breadcrumbs;

context.root.appendElement('div').appendText('Contents:');
var list = context.root.appendElement('div');
list.class = 'console';
context.globals['list'] = list;

var a = context.root.appendElement('a');
a.onclick = function() { context.ajax('mkdir'); }
a.appendText('Create Directory');

context.root.appendText(' | ');

a = context.root.appendElement('a');
a.onclick = function() { context.ajax('do_upload'); }
a.appendText('Upload File');

context.ajax('populate');

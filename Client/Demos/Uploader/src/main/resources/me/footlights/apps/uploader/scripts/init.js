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
a.appendText('Upload Something');

var status = context.root.appendElement('div');
context.globals['status'] = status;

context.root.appendElement('div').appendText('Uploaded files:');
var list = context.root.appendElement('div');
list.class = 'console';
context.globals['list'] = list;

context.ajax('populate');

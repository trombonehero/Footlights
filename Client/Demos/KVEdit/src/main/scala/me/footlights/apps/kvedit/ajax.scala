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
import java.net.URI

import me.footlights.api.{Preferences,WebRequest}
import me.footlights.api.ajax.{AjaxHandler,JavaScript,URLEncoded}
import me.footlights.api.ajax.JavaScript.sanitizeText
import me.footlights.api.support.Either._
import me.footlights.api.support.Regex._


package me.footlights.apps.kvedit {

/** Translates Ajax events to/from model events. */
class Ajax(app:KVEditor) extends AjaxHandler
{
	override def service(request:WebRequest) =
	{
		request.path() match
		{
			case Initialize =>
				refresh

			case CreateValue =>
				JavaScript log {
					app.createValue() fold (
						ex => "Error: " + ex.getMessage,
						value => value.toString
					)
				}
				refresh

			case DeleteValue(key) =>
				app delete key
				refresh

			case EditValue(key, currentValue, valueType) =>
				app.editValue(key, valueType, currentValue)
				refresh

			case other:String =>
				JavaScript log { "Unknown command '%s'" format other}
		}
	}

	private def refresh() = {
		val js = reset
		val foo = app.entries map entry
		app.entries map entry foreach js.append
		js append ("""
var addNew = context.root.appendElement('img');
addNew.src = 'images/oxygen/actions/document-new.png';
addNew.onclick = %s;
""" format { JavaScript ajax CreateValue asFunction })

		js
	}

	private val EntryTable = "context.globals['table']"

	private def reset =
		new JavaScript("""
context.root.clear();
var table = context.root.appendElement('table');
""" + EntryTable + """ = table;

var header = table.appendElement('thead').appendElement('tr');
header.appendElement('td').appendText('key');
header.appendElement('td').appendText('value');
""")

	private def entry[A](e:(String,A)) = e match { case (key,value) =>
		val onclick = JavaScript ajax EditValue(key, value, value.getClass.getSimpleName)
		val delete = JavaScript ajax DeleteValue(key)

		new JavaScript("""
var table = """ + EntryTable + """;
var row = table.appendElement('tr');

var key = row.appendElement('td').appendText('%s'); 
var value = row.appendElement('td').appendElement('a');
value.appendText('%s');
value.onclick = %s;

var del = row.appendElement('td').appendElement('img');
del.src = 'images/oxygen/actions/edit-delete.png';
del.style.height = '1em';
del.onclick = %s;
""" format (
			JavaScript sanitizeText key,
			JavaScript sanitizeText value.toString,
			onclick.asFunction,
			delete.asFunction
		))
	}

	private val Initialize  = "init"
	private val CreateValue = "create"
	private val DeleteValue = """delete/(\S+)""".r
	private val EditValue   = """edit_value/(\S+)/(\S+):(\S+)""".r
}

}

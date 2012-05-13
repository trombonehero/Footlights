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
import scala.collection.JavaConversions._

import me.footlights.api.{Preferences,WebRequest}
import me.footlights.api.ajax.{AjaxHandler,JavaScript,URLEncoded}
import me.footlights.api.ajax.JavaScript.sanitizeText
import me.footlights.api.support.Either._
import me.footlights.api.support.Regex._


package me.footlights.apps.fileman {

/** Translates Ajax events to/from model events. */
class Ajax(app:FileManager) extends AjaxHandler
{
	override def service(request:WebRequest) =
	{
		request.path() match
		{
			case Init =>
				new JavaScript append "context.load('scripts/init.js');"

			case PopulateView => refresh()

			case ChangeDirectory(URLEncoded(path)) =>
				setStatus {
					app chdir path fold ("error changing directory: " + _, path => "")
				} append {
					JavaScript ajax PopulateView
				}

			case MakeDirectory =>
				setStatus {
					app.mkdir fold (
						ex => "error: %s" format ex,
						name => "created directory '%s'" format name
					)
				} append {
					JavaScript ajax PopulateView
				}

			case UploadFile =>
				setStatus {
					app.upload map { _.name.toString } fold (
						ex => "Uploaded nothing: %s" format ex,
						"Downloaded '%s'." format _
					)
				}

			case Delete(URLEncoded(name)) =>
				app remove name fold (
					ex => setStatus { "error: %s" format ex.getMessage },
					success => JavaScript ajax PopulateView
				)

			case OpenFile(URLEncoded(name)) =>
				setStatus {
					app openFile name fold (
						ex => "error: %s" format ex.getMessage,
						f => "shared %s with another app" format f.toString
					)
				}

			case DownloadRequest(URLEncoded(name)) =>
				setStatus {
					app download name fold(
						ex => "Downloaded nothing (%s)" format ex,
						"Downloaded '%s'" format _.name.toString
					)
				}

			case other =>
				setStatus { "Unknown Ajax command '%s'" format other }
		}
	}

	private[fileman] def refreshView = fireAsynchronousEvent { refresh }

	private def refresh() = {
		val js = new JavaScript

		js append "var crumbs = context.globals['breadcrumbs'];"
		js append "crumbs.clear();"
		js append {
			app.breadcrumbs map { path =>
				val name = URLEncoded(path)
				addLink("crumbs", name.raw, JavaScript ajax { "chdir/%s" format name.encoded })
			} reduce {
				_ + "crumbs.appendText(' >> ');" + _
			}
		}

		js append "var list = context.globals['list'];"
		js append "list.clear();"
		app.listFiles map { entry =>
			val name = URLEncoded(entry.name)
			val ajax = JavaScript ajax {
				(if (entry.isDir) "chdir/%s" else "download/%s") format name.encoded
			}
			(name, ajax)
		} map { case (name, ajax) =>
			new JavaScript()
				.append("var line = list.appendElement('div');")
				.append("""
var del = line.appendElement('img');
del.src = 'images/oxygen/actions/edit-delete.png';
del.style.height = '1em';
del.onclick = %s;

var open = line.appendElement('img');
open.src = 'images/oxygen/actions/system-run.png';
open.style.height = '1em';
open.onclick = %s;

line.appendElement('span').appendText(' ');
""" format (JavaScript ajax Delete(name.encoded), JavaScript ajax OpenFile(name.encoded)))
				.append(addLink("line", name.raw, ajax))
		} foreach js.append

		js
	}

	private def addLink(parent:String, text:String, ajax:JavaScript) = """
(function() {
	var a = %s.appendElement('a');
	a.appendText('%s');
	a.onclick = %s;
})();
""" format (parent, JavaScript sanitizeText text, ajax)

	private def setStatus(unsafeText:String) =
		new JavaScript()
			.append("var status = context.globals['status']; status.clear();")
			.append("status.appendText('%s');" format (JavaScript sanitizeText unsafeText))

	private val Init = "init"
	private val PopulateView = "populate"
	private val ChangeDirectory = """chdir/(\S+)""".r
	private val MakeDirectory = "mkdir"
	private val UploadFile = "do_upload"
	private val Delete = """delete/(\S+)""".r
	private val OpenFile = """open/(\S+)""".r
	private val DownloadRequest = """download/(\S+)""".r
}

}

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
				addLink("crumbs", name.raw, "chdir/%s" format name.encoded)
			} reduce {
				_ + "crumbs.appendText(' >> ');" + _
			}
		}

		js append "var list = context.globals['list'];"
		js append "list.clear();"
		app.listFiles map { entry =>
			val name = URLEncoded(entry.name)
			if (entry.isDir) ("%s/" format name.raw, "chdir/%s" format name.encoded)
			else (name.raw, "download/%s" format name.encoded)
		} map { case (name, ajax) =>
			addLink("list.appendElement('div')", name, ajax)
		} foreach js.append

		js
	}

	private def addLink(parent:String, text:String, ajax:String) = """
(function() {
	var a = %s.appendElement('a');
	a.appendText('%s');
	a.onclick = %s;
})();
""" format (parent, JavaScript sanitizeText text, JavaScript ajax ajax)

	private def setStatus(unsafeText:String) =
		new JavaScript()
			.append("var status = context.globals['status']; status.clear();")
			.append("status.appendText('%s');" format (JavaScript sanitizeText unsafeText))

	private val Init = "init"
	private val PopulateView = "populate"
	private val ChangeDirectory = """chdir/(\S+)""".r
	private val MakeDirectory = "mkdir"
	private val UploadFile = "do_upload"
	private val DownloadRequest = """download/(\S+)""".r
}

}

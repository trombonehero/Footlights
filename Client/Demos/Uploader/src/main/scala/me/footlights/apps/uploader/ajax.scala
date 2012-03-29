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
import me.footlights.api.{Preferences,WebRequest}
import me.footlights.api.ajax.{AjaxHandler,JavaScript}
import me.footlights.api.ajax.JavaScript.sanitizeText

import java.net.{URI,URLEncoder,URLDecoder}

package me.footlights.apps.uploader {

/** Translates Ajax events to/from model events. */
class Ajax(app:Uploader) extends AjaxHandler
{
	def service(request:WebRequest) =
	{
		request.path() match
		{
			case "init" =>
				new JavaScript().append("context.load('scripts/init.js');")

			case "populate" =>
				val js = new JavaScript().append("var list = context.globals['list'];")
				app.storedNames foreach { name =>
					val sanitized = JavaScript sanitizeText name.toString
					val encoded = JavaScript sanitizeText URLEncoder.encode(name.toString, "utf-8")

					js.append("""
var a = list.appendElement('div').appendElement('a');
a.appendText('%s');
a.onclick = function() { context.ajax('download/%s)'); };
""" format (sanitized, encoded))
				}
				js

			case "do_upload" =>
				setStatus {
					app.upload map { _.name.toString } map { "Downloaded '%s'." format _ } getOrElse
						"Uploaded nothing (user may have clicked cancel)."
				}

			case DownloadRequest(name) =>
				setStatus {
					app download { URI create URLDecoder.decode(name, "utf-8") } map {
						"Downloaded '%s'" format _.name.toString
					} getOrElse
						"Downloaded nothing (user cancelled?)."
				}
		}
	}

	private def setStatus(unsafeText:String) =
		new JavaScript()
			.append("var status = context.globals['status']; status.clear();")
			.append("status.appendText('%s');" format (JavaScript sanitizeText unsafeText))

	private val DownloadRequest = """download/(\S+)""".r
}

}

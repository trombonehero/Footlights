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
import me.footlights.api.ajax.{AjaxHandler,JavaScript}
import me.footlights.api.ajax.JavaScript.sanitizeText
import me.footlights.apps.photos.PhotosApp;

package me.footlights.apps.photos {

/** Translates Ajax events to/from model events. */
class Ajax(app:PhotosApp) extends AjaxHandler
{
	def service(request:WebRequest) =
	{
		request.path() match
		{
			case "init" =>
				new JavaScript() append "context.load('scripts/init.js');"

			case "populate" =>
				val js = new JavaScript() append "context.globals['clear']();"
				app.savedPhotos foreach { js append addPhoto(_) }
				js

			case "do_upload" =>
				app.upload map { _.name.toString } map { filename =>
					setStatus {
						"Downloaded '%s'." format filename
					} append {
						"context.globals['new_photo']('%s');" format filename
					}
				} getOrElse setStatus { "Nothing uploaded (cancelled?)." }

			case RemoveImage(name) =>
				app remove new URI(name)
				new JavaScript() append "context.ajax('populate')"
		}
	}

	private def addPhoto(filename:URI) =
		new JavaScript().append("context.globals['new_photo']('%s');" format filename)

	private def setStatus(unsafeText:String) =
		new JavaScript()
			.append("var status = context.globals['status']; status.clear();")
			.append("status.appendText('%s');" format (JavaScript sanitizeText unsafeText))

	private val RemoveImage = """remove/(\S+)""".r
}

}

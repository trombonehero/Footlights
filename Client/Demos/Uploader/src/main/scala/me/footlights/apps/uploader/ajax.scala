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

package me.footlights.apps.uploader {

/** Translates Ajax events to/from model events. */
class Ajax(app:Uploader) extends AjaxHandler
{
	def service(request:WebRequest) =
	{
		request.path() match
		{
			case "do_upload" =>
				app.upload map { filename =>
					new JavaScript()
						.append("""
context.root.appendElement('div').appendText('Uploaded %s');""" format sanitizeText(filename))
				} getOrElse {
					new JavaScript()
						.append("""
context.root.appendElement('div').appendText('Nothing uploaded!');""")
				}
		}
	}
}

}

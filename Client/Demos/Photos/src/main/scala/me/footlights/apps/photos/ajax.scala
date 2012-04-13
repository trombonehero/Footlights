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

import me.footlights.apps.photos.PhotosApp;


package me.footlights.apps.photos {

class RegexFoo(r:scala.util.matching.Regex) {
	def substitute(values:String*) = {
		var s = r.pattern.pattern
		values foreach { value => s = s.replace("""(\S+)""", value) }
		s
	}
}

object RegexFoo {
	implicit def regex2regexFoo(r:scala.util.matching.Regex) = new RegexFoo(r)
}

import RegexFoo._

/** Translates Ajax events to/from model events. */
class Ajax(app:PhotosApp) extends AjaxHandler
{
	def service(request:WebRequest) =
	{
		request.path() match
		{
			case Initialize =>
				new JavaScript() append "context.load('scripts/init.js');"

			case RefreshTopView =>
				val js = clear
				js append addTool("Create album", JavaScript ajax CreateAlbum)
				app.albums foreach {
					case Right(album) =>
						js append addAlbum(
								album,
								JavaScript ajax { OpenAlbum substitute album.name },
								JavaScript ajax { DeleteAlbum substitute album.name }
							)

					case Left(ex) => setStatus { "Error opening album: %s" format ex }
				}
				js

			case CreateAlbum =>
				setStatus {
					app.create_album fold (
						ex => ex.getMessage,
						"Created directory: %s" format _
					)
				} append refreshTop

			case DeleteAlbum(name) =>
				setStatus {
					app.create_album fold (
						ex => ex.getMessage,
						"Created directory: %s" format _
					)
				} append refreshTop

			case OpenAlbum(URLEncoded(name)) =>
				val album = app album name
				album map { album =>
					val js = clear
					js append addTool("Back to albums", JavaScript ajax RefreshTopView)
					js append addTool("Add photo", JavaScript ajax { UploadImage substitute name })

					album.photos foreach { p =>
						js append """context.log('Photo: "%s"');""".format(p)
						js append addPhoto(p)
					}

					js append setStatus { "Opened album '%s'" format name }
					js
				} fold (
					ex => setStatus("Error: " + ex),
					js => js
				)

			case UploadImage(URLEncoded(album)) =>
				app album album map app.uploadInto

				setStatus { "what!?" } append {
					JavaScript ajax { OpenAlbum substitute album }
				}

			case RemoveImage(URLEncoded(name)) =>
				val path = (name split "/" toList) filter { !_.isEmpty }
				val album = app album { path.init reduce { _ + "/" + _ } }

				album tee {
					_ remove path.last } map {
					OpenAlbum substitute _.name
				} fold (
					ex => setStatus { "Error: %s" format ex },
					JavaScript.ajax
				)

			case other:String =>
				setStatus { "Unknown command '%s'" format other}
		}
	}

	private def clear() =
		new JavaScript append "context.globals['clear']();"

	private def addTool(text:String, ajax:JavaScript) = new JavaScript append { """
var span = context.globals['toolbar'].appendElement('span');
span.style.padding = '0.5em';

var a = span.appendElement('a');
a.appendText('%s');
a.onclick = %s;
""" format (JavaScript sanitizeText text, ajax.asFunction)
	}

	private def refreshTop() =
		new JavaScript append { JavaScript ajax RefreshTopView }

	private def addAlbum(album:Album, open:JavaScript, delete:JavaScript) =
		new JavaScript append "context.globals['new_album']('%s', '%s', %s, %s);".format(
			album.name, album.cover, open.asFunction, delete.asFunction
		)

	private def addPhoto(filename:String) =
		new JavaScript append "context.globals['new_photo']('%s', %s);".format(
			filename,
			JavaScript ajax { RemoveImage substitute filename } asFunction
		)

	private def setStatus(unsafeText:String) =
		new JavaScript()
			.append("var status = context.globals['status']; status.clear();")
			.append("status.appendText('%s');" format (JavaScript sanitizeText unsafeText))

	private val Initialize = "init"
	private val RefreshTopView = "populate"

	private val CreateAlbum = "create_album"
	private val DeleteAlbum = """delete_album/(\S+)""".r
	private val OpenAlbum = """album/(\S+)""".r

	private val UploadImage = """upload_image/(\S+)""".r
	private val RemoveImage = """delete_image/(\S+)""".r
}

}

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

import me.footlights.apps.photos.PhotosApp;


package me.footlights.apps.photos {

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
								JavaScript ajax OpenAlbum(album.name),
								JavaScript ajax ShareAlbum(album.name),
								JavaScript ajax DeleteAlbum(album.name)
							)

					case Left(ex) => setStatus { "Error opening album: %s" format ex }
				}
				js

			case CreateAlbum =>
				setStatus {
					app.createAlbum fold (
						ex => ex.getMessage,
						"Created directory: %s" format _
					)
				} append refreshTop

			case DeleteAlbum(URLEncoded(name)) =>
				setStatus {
					app deleteAlbum name fold (
						ex => "Error deleting album: %s" format ex,
						success => "Deleted album '%s'" format name
					)
				} append refreshTop

			case OpenAlbum(URLEncoded(name)) =>
				app album name map { album =>
					clear ::
					addTool("Back to albums", JavaScript ajax RefreshTopView) ::
					addTool("Add photo", JavaScript ajax UploadImage(name)) ::
					setStatus { "Opened album '%s'" format name } ::
					(album.photos map addPhoto toList)
				} fold (
					ex => setStatus("Error: " + ex),
					actions => actions reduce { _ append _ }
				)

			case UploadImage(URLEncoded(albumName)) =>
				app album albumName tee app.uploadInto map { _.name } map { OpenAlbum(_) } fold (
					ex => setStatus { "Error uploading photo: %s" format ex },
					JavaScript.ajax
				)

			case RemoveImage(URLEncoded(name)) =>
				val path = (name split "/" toList) filter { !_.isEmpty }
				val album = app album { path.init reduce { _ + "/" + _ } }

				album tee {
					_ remove path.last } map {
					_.name } map {
					OpenAlbum(_)
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
			JavaScript ajax RemoveImage(filename) asFunction
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

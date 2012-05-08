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
import java.util.NoSuchElementException
import java.util.logging.Logger

import scala.collection.JavaConversions._

import me.footlights.api.{Application,Directory,File,KernelInterface,ModifiablePreferences}
import me.footlights.api.support.Tee._
import me.footlights.apps.photos.Ajax;
import me.footlights.apps.photos.PhotosApp;

import me.footlights.api.support.Either._


package me.footlights.apps.photos {

class Album private(val name:String, dir:Directory) {
	override def toString() = "Album { '%s', %s }" format (name, dir.toString)

	def cover = dir open "cover" fold (ex => Album.DefaultCoverImage, _.name.toString)
	def photos =
		dir.entries filter { !_.isDir } map { entry => "/%s/%s" format (name, entry.name) }

	def add(file:File) = dir save ("photo-%d" format System.currentTimeMillis, file)
	def remove(name:String) = dir remove name map { dir => this }
}

object Album {
	def apply(name:String, dir:Directory) = new Album(name, dir)
	def apply(e:Directory.Entry) = e.directory map { new Album(e.name, _) }

	private val DefaultCoverImage = "images/oxygen/actions/view-preview.png"
}


/**
 * An application which uploads user-selected files into the Content-Addressed Store.
 * @author Jonathan Anderson <jon@footlights.me>
 */
class PhotosApp(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) extends Application
{
	def ajaxHandler = new Ajax(this)
	val shortName = "Photos"

	def albums() = root.entries filter { _.isDir } map Album.apply
	def album(name:String) = root openDirectory name map { Album(name, _) }

	def createAlbum =
		kernel promptUser "Album name?" flatMap { name =>
			root mkdir name map {
				Album(name, _)
			}
		}

	def deleteAlbum(name:String) = root remove name map { success => this }

	def uploadInto(album:Album) = {
		kernel.openLocalFile flatMap album.add
	}

	// If get() fails (a serious error), an exception will be thrown to propagate up the stack.
	private val root = kernel openDirectory "" get
}


/** Builder used by Footlights to find and initialize the app. */
object PhotosApp
{
	def init(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) =
		new PhotosApp(kernel, prefs, log)
}

}

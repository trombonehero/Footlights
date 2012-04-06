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

import me.footlights.api.{Application,File,KernelInterface,ModifiablePreferences}
import me.footlights.api.support.Tee._
import me.footlights.apps.photos.Ajax;
import me.footlights.apps.photos.PhotosApp;

import me.footlights.api.support.Either._


package me.footlights.apps.photos {

/**
 * An application which uploads user-selected files into the Content-Addressed Store.
 * @author Jonathan Anderson <jon@footlights.me>
 */
class PhotosApp(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) extends Application
{
	def ajaxHandler = new Ajax(this)

	private[photos] def upload() = kernel.openLocalFile tee store
	private[photos] def savedPhotos = (prefs getString Photos map split flatten) map { new URI(_) }

	private[photos] def remove(name:URI) = prefs.synchronized {
		setFilenames { savedPhotos filter { _ != name } }
	}

	private def store(file:File) = prefs.synchronized {
		setFilenames { (savedPhotos toList) :+ file.name }
	}

	private def setFilenames(files:Iterable[URI]) = prefs.synchronized {
		val packed = files match {
			case Nil => ""
			case i:Iterable[_] => i map { _.toString } reduce join
		}

		prefs.set(Photos, packed)
	}

	private def join(x:String, y:String) = x + ";" + y
	private def split(x:String) = (x split ";" toList) filter { !_.isEmpty }

	private val Photos = "photos"
}


/** Builder used by Footlights to find and initialize the app. */
object PhotosApp
{
	def init(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) =
		new PhotosApp(kernel, prefs, log)
}

}

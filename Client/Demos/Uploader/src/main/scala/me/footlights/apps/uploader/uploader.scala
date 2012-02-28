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

package me.footlights.apps.uploader {

/**
 * An application which uploads user-selected files into the Content-Addressed Store.
 * @author Jonathan Anderson <jon@footlights.me>
 */
class Uploader(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) extends Application
{
	def ajaxHandler = new Ajax(this)

	private[uploader] def upload() = kernel.openLocalFile tee store
	private[uploader] def download(name:String) =
		kernel open name tee kernel.saveLocalFile

	private[uploader] def storedNames =
		(prefs getString SaveList map split flatten) map { new URI(_) }

	private def store(file:File) = prefs.synchronized {
		prefs.set(SaveList, (storedNames toList) :+ file.name map { _.toString } reduce join)
	}

	private def join(x:String, y:String) = x + ";" + y
	private def split(x:String) = x split ";" toList

	private val SaveList = "saved_files"
}


/** Builder used by Footlights to find and initialize the app. */
object Uploader
{
	def init(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) =
		new Uploader(kernel, prefs, log)
}

}

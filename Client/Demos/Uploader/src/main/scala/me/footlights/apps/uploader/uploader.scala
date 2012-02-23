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
import java.util.NoSuchElementException
import java.util.logging.Logger

import me.footlights.api.{Application,KernelInterface,ModifiablePreferences}

package me.footlights.apps.uploader {

/**
 * An application which uploads user-selected files into the Content-Addressed Store.
 * @author Jonathan Anderson <jon@footlights.me>
 */
class Uploader(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) extends Application
{
	def ajaxHandler = new Ajax(this)

	private[uploader] def upload() = {
		val file = kernel.openLocalFile
		file map { _.name } foreach storeName
		file
	}

	private def storedNames = prefs.getString(SAVE_LIST) map { _ split ":" toList } getOrElse Nil

	private def storeName(name:String) = prefs.synchronized {
		prefs.set(SAVE_LIST, name :: storedNames reduceLeft { _ + ":" + _ })
	}

	private val SAVE_LIST = "saved_files"
}


/** Builder used by Footlights to find and initialize the app. */
object Uploader
{
	def init(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) =
		new Uploader(kernel, prefs, log)
}

}

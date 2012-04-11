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
import me.footlights.api.support.Either._
import me.footlights.api.support.Tee._


package me.footlights.apps.uploader {

/**
 * An application which uploads user-selected files into the Content-Addressed Store.
 * @author Jonathan Anderson <jon@footlights.me>
 */
class Uploader(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) extends Application
{
	def ajaxHandler = new Ajax(this)

	private[uploader] def listFiles() = cwd.entries
	private[uploader] def chdir(dir:String) = {
		val newdir =
			if (dir startsWith "/") kernel openDirectory dir
			else cwd openDirectory dir

		newdir flatMap { newcwd =>
			cwd = newcwd
			currentPath = dir split "/" toList match {
				case "" :: absolutePath => absolutePath
				case Nil => Nil
				case relativePath => currentPath ++ relativePath
			}
			Right(dir)
		}
	}

	private[uploader] def mkdir() = kernel promptUser ("Directory name", None) tee cwd.mkdir

	private[uploader] def breadcrumbs = currentPath.scan("/")(_ + _ + "/")

	private[uploader] def upload() = kernel.openLocalFile tee store
	private[uploader] def download(filename:String) = cwd open filename tee kernel.saveLocalFile

	private def store(file:File) =
		kernel promptUser ("File name", None) map { cwd.save(_, file) }

	private def join(x:String, y:String) = x + ";" + y
	private def split(x:String) = x split ";" toList

	private var cwd = kernel openDirectory "/" get
	private var currentPath = List[String]()
}


/** Builder used by Footlights to find and initialize the app. */
object Uploader
{
	def init(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) =
		new Uploader(kernel, prefs, log)
}

}

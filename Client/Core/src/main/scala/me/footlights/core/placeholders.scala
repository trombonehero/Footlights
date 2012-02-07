/*
 * Copyright 2011 Jonathan Anderson
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
import java.io.{ByteArrayOutputStream, IOException}
import java.nio.ByteBuffer
import java.net.URI
import java.util.logging.{Level, Logger}
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import me.footlights.api.Application
import me.footlights.api.File
import me.footlights.api.KernelInterface
import me.footlights.api.ModifiablePreferences
import scala.annotation.tailrec

package me.footlights.core {

import apps.AppStartupException
import apps.AppWrapper
import crypto.Keychain


/** Evaluates placeholder values. */
trait Placeholders extends Footlights {
	protected def prefs:me.footlights.api.Preferences

	override def evaluate(name:String) = {
		val collection :: id = name split('.') toList
		val prefKey = collection match {
			case "user" => "profileData"
			case _ => ""
		}

		prefs getString(prefKey) flatMap { filename =>
			try { Option(open(filename)) } catch { case e:data.NoSuchBlockException => None }
		} map {
			_ match { case file:data.File => Preferences.parse(file getContents) }
		} map {
			_ get { id reduceLeft { _ + _ } }
		} getOrElse "(unknown)"
	}
}

}
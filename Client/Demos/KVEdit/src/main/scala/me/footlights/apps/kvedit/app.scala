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

import me.footlights.api
import me.footlights.api.support.Either._
import me.footlights.api.support.Tee._

import me.footlights.core


package me.footlights.apps.kvedit {

/**
 * An application which edits Footlights key-value stores.
 * @author Jonathan Anderson <jon@footlights.me>
 */
class KVEditor(kernel:api.KernelInterface, appPrefs:api.ModifiablePreferences, log:Logger)
	extends api.Application("KV Editor")
{
	val uses = (appPrefs getInt "uses" map { i:java.lang.Integer => i:Int } getOrElse 0)
	appPrefs.set("uses", uses + 1)

	private val ajax = new Ajax(this)
	override val ajaxHandler = Some(ajax)

	override def open(d:api.Directory) = {
		val parsed = d open AttributesFilename map { _.copyContents } map core.Preferences.parse
		val saveModified = (b:java.nio.ByteBuffer) => d.save(AttributesFilename, b)

		parsed map { x =>
			synchronized { prefs = core.ModifiableStorageEngine(x, Some(saveModified)) }
		} fold (
			ex => ajax fire ex,
			success => ajax.refreshView
		)
	}

	def entries(): Map[String,_] = {
		val prefs = synchronized { this.prefs }

		prefs.keys map { entry => (entry.getKey, entry.getValue) } map { case (key, valueType) =>
			import api.Preferences.PreferenceType._

			val value = valueType match {
				case BOOLEAN => prefs getBoolean key get
				case STRING  => prefs getString key get
				case FLOAT   => prefs getFloat key get
				case INTEGER => prefs getInt key get
			}

			key -> value
		} toMap
	}

	def createValue() = {
		kernel promptUser "Key?" flatMap { key =>
			kernel promptUser { "Value for '%s'?" format key } map { value =>
				setValue(key, value)
				key -> value
			}
		}
	}

	def delete(key:String) = prefs delete key

	def editValue(key:String, typeName:String, currentValue:String = "") = {
		kernel promptUser key flatMap { userInput => setValue(key, userInput, typeName) }
	}

	var prefs = appPrefs

	private def setValue(key:String, value:String, typeName:String = "String") =
		typeName match {
			case Boolean => prefs.set(key, value.toBoolean) ; Right(value.toBoolean)
			case Int     => prefs.set(key, value.toInt)     ; Right(value.toInt)
			case Float   => prefs.set(key, value.toFloat)   ; Right(value.toFloat)
			case String  => prefs.set(key, value)           ; Right(value)
			case other   => Left(new Exception("Invalid type name '%s'" format typeName))
		}

	private val Boolean = classOf[Boolean].getSimpleName
	private val Int = classOf[Int].getSimpleName
	private val Float = classOf[Float].getSimpleName
	private val String = classOf[String].getSimpleName

	private val AttributesFilename = "attributes"
}


/** Builder used by Footlights to find and initialize the app. */
object KVEditor {
	def init(kernel:api.KernelInterface, prefs:api.ModifiablePreferences, log:Logger) =
		new KVEditor(kernel, prefs, log)
}

}

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
package me.footlights.android

import scala.collection.JavaConversions._

import me.footlights.core.PreferenceStorageEngine


class PreferenceAdapter private (private val prefs:android.content.SharedPreferences)
	extends PreferenceStorageEngine {

	override def getAll = prefs.getAll

	override def getRaw(key:String) =
		if (prefs contains key) Some(prefs.getAll get key toString)
		else None

	override def getString(key:String) =
		if (prefs contains key) Some(prefs.getString(key, ""))
		else None

	override def getBoolean(key:String) =
		if (prefs contains key) Some(prefs.getBoolean(key, false))
		else None

	override def getInt(key:String) =
		if (prefs contains key) Some(prefs.getInt(key, 0))
		else None

	override def getFloat(key:String) =
		if (prefs contains key) Some(prefs.getFloat(key, 0))
		else None
}


object PreferenceAdapter {
	def wrap(prefs:android.content.SharedPreferences) = new PreferenceAdapter(prefs)

}

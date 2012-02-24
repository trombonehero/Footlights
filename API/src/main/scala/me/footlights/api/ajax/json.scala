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
package me.footlights.api.ajax {

/**
 * JavaScript Object Notation, a common data serialization format.
 *
 * @see RFC 4627; http://www.ietf.org/rfc/rfc4627.txt
 */
final object JSON {
	/**
	 * Construct a JSON object from an Iterable of (key->value) pairs.
	 *
	 * To convert a Map to a JSON object, use the implicit conversion {@link #map2json}.
	 */
	def apply(members:(String,JSONData)*) = new JSON(members toMap)

	// Primitive member types.
	implicit def int2json(i:Int)       = JSONData(i toString)
	implicit def double2json(d:Double) = JSONData(d toString)
	implicit def str2json(x:String)    = JSONData(quote(x))

	// Arrays and iterables become JSON lists.
	implicit def array2json[T <% JSONData](x:Array[T]) = iterable2json(x toIterable)
	implicit def iterable2json[T <% JSONData](x:Iterable[T]) =
		JSONData("[ " + (x map { _.repr } reduce { _ + ", " + _ }) + " ]")

	// Maps (and pairs, which are map entries) become JSON objects.
	implicit def pair2json[T <% JSONData](x:(String,T)) = map2json(Map(x))
	implicit def map2json[T <% JSONData](x:Map[String,T]) =
		new JSON(x map { entry =>
				val value:JSONData = entry._2      // compiler hint: we want a conversion!
				(entry._1, value)
			})

	/** Escape and quote a {@link String}. */
	private[ajax] def quote(s:String) = "\"" + s.replaceAll("\"", "\\\"") + "\""
}

/** A value which can be stored in a {@link JSON} object's key->value mapping. */
case class JSONData(repr:String = "null")

final class JSON private(members:Map[String,JSONData]) extends JSONData with AjaxResponse
{
	def this() = this(Map())

	override val mimeType = "application/json"
	override lazy val data = new java.io.ByteArrayInputStream(getBytes)
	override lazy val toString = repr

	override val repr = {
		val fields = for ((key, value) <- members) yield JSON.quote(key) + ": " + value.repr
		"{ " + (fields reduce { _ + ", " + _ }) + " }"
	}

	lazy val getBytes = repr getBytes

	def +(member:(String,JSONData)) = new JSON(members + member)
	def ++(members:Map[String,JSONData]) = new JSON(this.members ++ members)

	// Java-friendly names.
	import JSON._

	def plus(key:String, value:Int) = this + (key -> value)
	def plus(key:String, value:Double) = this + (key -> value)
	def plus(key:String, value:String) = this + (key -> value)

	def merge(map:Map[String,JSONData]) = this ++ map
}

}

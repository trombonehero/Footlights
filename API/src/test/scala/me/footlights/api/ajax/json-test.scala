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
import scala.collection.JavaConversions._

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito._

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers._
import org.scalatest.mock.MockitoSugar

import org.junit.runner.RunWith


package me.footlights.api.ajax {

import JSON._
import JSONData._

/** Integration tests for {@link ClasspathLoader}. */
@RunWith(classOf[JUnitRunner])
class JSONTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {
	"A JSON object should be able to hold " - {
		"the empty set" in {
			JSON().toString should fullyMatch regex """\{[ ]*\}"""
		}

		"primitive members" in {
			val json = JSON("foo" -> 42, "bar" -> 3.1415926, "baz" -> "Hello, world!")
			json.toString should equal (
					"{ \"foo\": 42, \"bar\": 3.1415926, \"baz\": \"Hello, world!\" }")
		}

		"arrays" in {
			val arr = List(1,2,3) toArray
			val json = JSON("array" -> arr)
			json.toString should equal ("{ \"array\": [ 1, 2, 3 ] }")
		}

		"lists" in {
			val json = JSON("list" -> List(1, 2, 3))
			json.toString should equal ("{ \"list\": [ 1, 2, 3 ] }")
		}

		"lists of lists" in {
			val json = JSON("listoflists" -> List(1 ::  2 :: 3 :: Nil))
			json.toString should equal ("{ \"listoflists\": [ [ 1, 2, 3 ] ] }")
		}

		"explicit JSON objects" in {
			val json = JSON("sub" -> JSON("foo" -> "bar"))
			json.toString should equal ("{ \"sub\": { \"foo\": \"bar\" } }")
		}

		"maps" in {
			val json = JSON("sub" -> Map("foo" -> "bar"))
			json.toString should equal ("{ \"sub\": { \"foo\": \"bar\" } }")
		}

		"maps as implicit JSON objects" in {
			val json = JSON("sub" -> ("foo" -> "bar"))
			json.toString should equal ("{ \"sub\": { \"foo\": \"bar\" } }")
		}

		"anything a Map[String,T] can hold" in {
			val map:Map[String,JSONData] = Map(
					"foo" -> 42,
					"bar" -> "hello",
					"baz" -> List(1, 2),
					"map" -> Map("a" -> 1, "b" -> 2)
				)
			val json:JSON = map
			json.toString should equal (
				"""{ "foo": 42, "bar": "hello", "baz": [ 1, 2 ], "map": { "a": 1, "b": 2 } }""")
		}
	}
}

}

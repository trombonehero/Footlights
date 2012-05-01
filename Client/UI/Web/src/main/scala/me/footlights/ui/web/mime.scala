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

package me.footlights.ui.web

object MimeType {
	def apply(path:String):String = {
		path match {
			case CSS(_)      => "text/css"
			case HTML(_)     => "text/html"
			case JS(_)       => "text/javascript"

			case GIF(_)      => "image/gif"
			case JPEG(_)     => "image/jpeg"
			case PNG(_)      => "image/png"

			case TrueType(_) => "font/ttf"

			case _           => "application/octet-stream"
		}
		
	}

	private val CSS      = """(.*)\.css""".r
	private val GIF      = """(.*)\.gif""".r
	private val HTML     = """(.*)\.html?""".r
	private val JPEG     = """(.*)\.jpe?g""".r
	private val JS       = """(.*)\.js""".r
	private val PNG      = """(.*)\.png""".r
	private val TrueType = """(.*)\.ttf""".r
}

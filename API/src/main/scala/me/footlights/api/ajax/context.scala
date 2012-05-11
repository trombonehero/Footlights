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
package me.footlights.api.ajax

import me.footlights.api


/** Handle to a client-side context (ECMAScript sandbox or 'window'). */
class Context(defaultHandler:Option[AjaxHandler] = None) extends AjaxHandler {
	def this() = this(None)

	override final def service(request:api.WebRequest) =
		handlers get request.prefix orElse defaultHandler map { _ service request.shift } getOrElse {
			throw new IllegalArgumentException(
				"No handler for request '" + request.prefix + "' registered in " + this)
		}

	final def register(name:String, handler:AjaxHandler):Any = register(name -> handler)
	final def register(x:(String,AjaxHandler)) = synchronized { handlers += x }
	final def unloadHandlers = synchronized { handlers = Map() }

	final override def toString =
		("{ handlers: [ " :: handlers.values.toList map { _.toString }) :+ " ] }" reduce { _ + _ }

	private var handlers = Map[String,AjaxHandler]()
}


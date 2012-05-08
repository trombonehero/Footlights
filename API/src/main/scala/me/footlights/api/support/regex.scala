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
package me.footlights.api.support


class SubstitutableRegex(r:scala.util.matching.Regex) {
	def apply(values:Any*) = substitute(values : _*)
	def substitute(values:Any*) =
		(r.pattern.pattern /: (values map { _.toString })) { _.replace("""(\S+)""", _) }
}

object Regex {
	implicit def regex2substitutable(r:scala.util.matching.Regex) = new SubstitutableRegex(r)
}

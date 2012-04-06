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


/**
 * Provide no-nonsense foreach, map and flatMap methods to Either.
 *
 * Rather than (((foo.right map f).right map g).right map h), provide foreach, map and flatMap
 * like Option. This is sensible because we always use Either[exceptional case, normal case],
 * so map ought to normally operate on the right case.
 */
class MappableEither[A,B](e: Either[A,B]) {
	def foreach(f:B => Any): Unit = e.right foreach f

	def map[C](f:B => C): Either[A,C] = e.right map f
	def flatMap[C](f:B => Either[A,C]): Either[A,C] = e.right flatMap f
}

object Either {
	implicit def either2mappable[A,B](e: Either[A,B]) = new MappableEither(e)
}

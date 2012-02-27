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
package me.footlights.api.support;


/**
 * Add a "tee" method to {@link Option} which:
 *  - applies a given function and
 *  - returns the original {@link Option}.
 *
 * This is conceptually similar to the UNIX tee(1) command, which copies
 * input to both a pipe and and output file. I find it to be useful.
 */
class Tee[A](o: Option[A]) {
	def tee[B](f:A => B): Option[A] = {
		o foreach f
		o
	}
}

object Tee {
	implicit def opt2tee[A](o: Option[A]) = new Tee(o)
}

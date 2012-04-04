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

import org.mockito


package me.footlights.core {

/**
 * An argument matcher which matches on an arbitrary predicate.
 */
class PredicateMatcher[A](test:A => Boolean) extends mockito.ArgumentMatcher[A] {
	def matches(a:Any) = test(a.asInstanceOf[A])
}

/**
 * Implicit conversions into argument {@link mockito.Matcher}s.
 */
object CustomMatchers {
	implicit def test2matcher[A](test:A => Boolean) =
		mockito.Matchers.argThat(new PredicateMatcher(test))
}

}

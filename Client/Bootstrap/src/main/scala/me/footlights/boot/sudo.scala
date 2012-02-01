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
import java.security.{AccessController, PrivilegedActionException, PrivilegedExceptionAction}


package me.footlights.boot {

/**
 * Represents JVM privilege.
 *
 * This is, unfortunately, duplicate code (as in "copy-and-paste"), but it's very small, and
 * it seems an insurmountable obstacle to load the Core version without the privilege that we
 * need to load the Core version. 
 */
object Sudo {
	/**
	 * Execute a function with JVM privilege (using {@link AccessController}).
	 *
	 * The name "sudo" is meant to be evocative of privilege in general;
	 * it does not refer specifically to system privilege as conferred by sudo(8).
	 */
	def sudo[T](code:() => T):T =
		try AccessController.doPrivileged[T] {
			new PrivilegedExceptionAction[T]() { override def run:T = code() }
		}
		catch {
			case e:PrivilegedActionException => throw e getCause
		}
}

}

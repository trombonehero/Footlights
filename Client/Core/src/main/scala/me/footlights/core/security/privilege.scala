/*
 * Copyright 2011-2012 Jonathan Anderson
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
import java.net.URI
import java.nio.ByteBuffer

import me.footlights.api.{File,KernelInterface}
import me.footlights.core.Footlights
import me.footlights.core.crypto.Link


package me.footlights.core.security {

/**
 * A trait which invokes kernel operations (syscalls) with JVM privilege.
 *
 * This allows privileged operations, such as file I/O, to be invoked from unprivileged
 * application code.
 *
 * Obviously, the privileged {@link Kernel} must be carefully implemented to avoid abuse. 
 */
trait KernelPrivilege extends Footlights {
	abstract override def open(name:URI)           = Privilege.sudo { () => super.open(name) }
	abstract override def openLocalFile()          = Privilege.sudo { () => super.openLocalFile() }
	abstract override def open(link:Link)          = Privilege.sudo { () => super.open(link) }
	abstract override def save(data:ByteBuffer)    = Privilege.sudo { () => super.save(data) }
	abstract override def saveLocalFile(f:File)    = Privilege.sudo { () => super.saveLocalFile(f) }
	abstract override def promptUser(prompt:String, title:String, default:Option[String]) =
		Privilege.sudo { () => super.promptUser(prompt, title, default) }
}

/** Represents JVM privilege. */
object Privilege {
	/**
	 * Execute a function with JVM privilege (using {@link AccessController}).
	 *
	 * The name "sudo" is meant to be evocative of privilege in general;
	 * it does not refer specifically to system privilege as conferred by sudo(8).
	 */
	private[core] def sudo[T](code:() => T) =
		try AccessController.doPrivileged[T] {
			new PrivilegedExceptionAction[T]() { override def run:T = code() }
		}
		catch {
			case e:PrivilegedActionException => throw e getCause
		}
}

}

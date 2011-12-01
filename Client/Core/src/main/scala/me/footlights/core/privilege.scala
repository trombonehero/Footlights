package me.footlights.core

import _root_.java.security.{AccessController, PrivilegedActionException, PrivilegedExceptionAction}
import _root_.java.nio.ByteBuffer

import _root_.me.footlights.plugin.KernelInterface


/**
 * A trait which invokes kernel operations (syscalls) with JVM privilege.
 *
 * This allows privileged operations, such as file I/O, to be invoked from unprivileged
 * application code.
 *
 * Obviously, the privileged {@link Kernel} must be carefully implemented to avoid abuse. 
 */
trait KernelPrivilege extends Kernel {
	abstract override def generateUUID             = Privilege.sudo { () => super.generateUUID }

	abstract override def open(name:String)        = Privilege.sudo { () => super.open(name) }
	abstract override def openLocalFile()          = Privilege.sudo { () => super.openLocalFile() }
	abstract override def save(data:ByteBuffer)    = Privilege.sudo { () => super.save(data) }

}

/**
 * Represents JVM privilege.
 *
 * TODO: Once we've sorted out the ClassLoader issues referenced in {@link IO#read}, make this
 *       a private def within KernelPrivilege. Privilege should only need to be applied at the
 *       kernel level, where it should always be mixed in via KernelPrivilege. 
 */
object Privilege {
	/**
	 * Execute a function with JVM privilege (using {@link AccessController}).
	 *
	 * The name "sudo" is meant to be evocative of privilege in general;
	 * it does not refer specifically to system privilege as conferred by sudo(8).
	 */
	def sudo[T](code:() => T) =
		try AccessController.doPrivileged[T] {
			new PrivilegedExceptionAction[T]() { override def run:T = code() }
		}
		catch {
			case e:PrivilegedActionException => throw e getCause
		}
}

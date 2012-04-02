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
package me.footlights.core

import java.net.URI

import me.footlights.api

import apps.AppWrapper
import crypto.Link
import data.{Directory,File}
import data.store.Stat


/** Interface to the software core. */
trait Footlights extends api.KernelInterface {
	/** Open a particular {@link Link}. */
	def open(link:Link): Option[File]

	/** Open a directory by {@link Link}. */
	def openDirectory(link:Link): Option[Directory]

	/** Open a directory whose decryption key is known to the kernel. */
	def openDirectory(name:URI): Option[Directory]

	/** Open a hierarchical name, relative to a base {@link Directory}. */
	def openat(path:Iterable[String], base:Directory): Option[File]

	/** Save a generated {@link File} to the filesystem. */
	def save(file:File): Option[File]

	/** Save an immutable {@link Directory} to the filesystem. */
	def save(dir:Directory): Option[Directory]

	/** Save data to a local {@link java.io.File}. */
	def saveLocal(file:File, filename:java.io.File)

	/**
	 * Convert a placeholder name (e.g. "user.name") into a meaningful value.
	 *
	 * This is part of {@link Footlights} rather than the {@link KernelInterface} because apps
	 * cannot request placeholder evaluation directly; it has to be done by a trusted bit of UI
	 * code, which inserts the proxied content in such a way that the app UI can't read it.
	 */
	def evaluate(placeholder:String): String

	def registerUI(ui:UI)
	def deregisterUI(ui:UI)

	def localizeJar(uri:URI): Option[java.util.jar.JarFile]

	/**
	 * List some of the {@link File} names which are known to exist in the {@link Store}.
	 *
	 * This list describes files stored in local cache, not remotely on the global CAS.
	 */
	def listFiles: Iterable[Stat]

	def runningApplications(): Seq[AppWrapper]
	def loadApplication(uri:URI): Either[Exception,AppWrapper]
	def unloadApplication(app:AppWrapper)
}

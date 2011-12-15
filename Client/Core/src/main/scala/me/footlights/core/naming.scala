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
import java.net.URL
import java.nio.ByteBuffer

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.parsing.json.JSON

import sun.net.www.protocol.http.HttpURLConnection

import org.apache.commons.codec.binary.Hex

package me.footlights.core {

import crypto.{Fingerprint,Keychain,SecretKey}
import data.Link

/**
 * Resolves public names (e.g. a {@link URL}) into Footlights names (e.g. a {@link Fingerprint}
 * or a {@link Link}).
 */
class Resolver(io:IO, keychain: Keychain)
{
	/**
	 * Resolve a URL into a {@link Link}.
	 *
	 * The URL should be for a JSON resource which contains at least a "hash" member. The JSON
	 * may optionally contain a "key" member as well, but if not, we expect the relevant key
	 * to already be in our {@link Keychain}.
	 */
	def resolve(url: URL):Link = {
		// Turn the remote content into a {@link String}
		Some(io fetch { url } getContents) map { b => {
				val bytes = new Array[Byte](b.remaining)
				b.get(bytes)
				new String(bytes)
			}
		// Parse the JSON
		} flatMap { JSON.parseFull(_) } map { _ match {
			case m:Map[String,Any] => m
			case a:Any => Map()
		} } map { json =>
			// Decode fingerprint
			(json.get("fingerprint") match {
				case s:String => Some(Fingerprint.decode(s))
				case a:Any => None
			}) map {
				json.get("key") match {
					// If a key has been explicitly specified, use it.
					case s:String => {
						val tokens = s.split(":")
						tokens.length match {
							case 2 =>
								Link.newBuilder
									.setFingerprint(_)
									.setKey(
										SecretKey.newGenerator
											.setAlgorithm(tokens(0))
											.setBytes(Hex.decodeHex(tokens(1).toCharArray()))
											.generate)
									.build

							case _ => null
						}
					}

					// Otherwise, we expect the key to be in the keychain.
					case _ => keychain getLink _
				}
			} get
		} getOrElse null
	}
}

object Resolver {
	def apply(io:IO, keychain: Keychain) = new Resolver(io, keychain)
}

}

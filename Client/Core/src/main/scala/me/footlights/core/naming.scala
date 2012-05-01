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

import me.footlights.api.support.Either._


package me.footlights.core {

import crypto.{Fingerprint,Keychain,Link,SecretKey}

/**
 * Resolves public names (e.g. a {@link URL}) into Footlights names (e.g. a {@link Fingerprint}
 * or a {@link Link}).
 */
class Resolver private(io:IO, keychain: Keychain)
{
	/**
	 * Resolve a URL into a {@link Link}.
	 *
	 * The URL should be for a JSON resource which contains at least a "hash" member. The JSON
	 * may optionally contain a "key" member as well, but if not, we expect the relevant key
	 * to already be in our {@link Keychain}.
	 */
	def resolve(url: URL):Either[Exception,Link] = {
		fetchJSON(url) flatMap { json =>
			json get "fingerprint" toRight(
				new Exception("No fingerprint in %s" format json)) map {
				case s:String => Fingerprint decode s
			} flatMap { f =>
				json get "key" map {
					case s:String =>
						s split ":" toList match {
							case algorithm :: bytes :: Nil =>
								Right(Link.newBuilder
									.setFingerprint(f)
									.setKey(
										SecretKey.newGenerator
											.setAlgorithm(algorithm)
											.setBytes(Hex decodeHex bytes.toCharArray)
											.generate)
									.build)

							case a:Any =>
								Left(new Exception("Key should be alg:bytes, not '%s'" format a))
						}

					case a:Any =>
						Left(new Exception("Key in JSON was not a string: '%s'" format a))
				} getOrElse {
					keychain getLink f toRight(new Exception("No key in JSON or keychain"))
				}
			}
		}
	}

	def fetchJSON(url: URL):Either[Exception,Map[String,_]] =
		io fetch url map { _.copyContents } map { buffer =>
			val bytes = new Array[Byte](buffer.remaining)
			buffer.get(bytes)

			new String(bytes)
		} map JSON.parseFull flatMap {
			// Convert (Any->Any) mapping into (String->Any).
			case Some(m:Map[_,_]) => Right(for ((k,v) <- m) yield (k.toString(), v))
			case _ => Left(new Exception("Unable to parse JSON at %s" format url))
		}
}

object Resolver {
	def apply(io:IO, keychain: Keychain) = new Resolver(io, keychain)
}

}

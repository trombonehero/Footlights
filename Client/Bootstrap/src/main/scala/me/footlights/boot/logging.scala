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
package me.footlights.boot

import java.io.{PrintWriter,StringWriter}
import java.text.MessageFormat
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.Date


class LogFormatter extends Formatter {
	override def format(record:LogRecord) = {
		val except = record.getThrown match {
			case t:Throwable =>
				val s = new StringWriter()
				t printStackTrace new PrintWriter(s)
				"Cause: %s\n" format s

			case _ => ""
		}

		"%8s %-8s %14s.%-14s %s\n%s" format (
				date.format(new Date(record.getMillis)),
				record.getLevel,
				Some(record.getLoggerName) map { s => s.substring(s.lastIndexOf('.') + 1) } get,
				record.getSourceMethodName,
				record.getMessage,
				except
			)
	}

	private val date = new java.text.SimpleDateFormat("k:mm:ss")
	private val template = new MessageFormat("{3,date,h:mm:ss}{0}[{1}|{2}|]: {4} \n");
}
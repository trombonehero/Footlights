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
package me.footlights.ui

import javax.swing
import me.footlights.core


class SwingUI(footlights:core.Footlights) extends core.UI("Swing UI", footlights) {

	override def run = textAreaPrintStream println "Swing UI started"
	override def handleEvent(event:core.UI.Event) = {}

	private val frame = new swing.JFrame("Footlights")
	frame setDefaultCloseOperation swing.JFrame.EXIT_ON_CLOSE

	private val appList = new ApplicationList(footlights)
	frame.getContentPane.add(appList, java.awt.BorderLayout.LINE_START)

	private val textArea = new swing.JTextArea
	textArea setBorder swing.BorderFactory.createLoweredBevelBorder
	textArea setBackground java.awt.Color.BLACK
	textArea setForeground java.awt.Color.GRAY

	private val scrollPane = new swing.JScrollPane(textArea)
	frame.getContentPane.add(scrollPane, java.awt.BorderLayout.CENTER)

	frame.pack
	frame.setBounds(0, 0, 800, 600)
	frame setVisible true

	private val textAreaPrintStream = new java.io.PrintStream(
		new java.io.OutputStream {
			override def write(b:Int) = {
				textArea append new String(List(b) map { _.toChar } toArray)
				textArea setCaretPosition textArea.getDocument.getLength
			}
		}
	)
	System setOut textAreaPrintStream
	System setErr textAreaPrintStream
}

object SwingUI {
	def init(footlights:core.Footlights) = new SwingUI(footlights)
}

/*
import java.io.*;
import javax.swing.*;
*/

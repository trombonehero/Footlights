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
package me.footlights.ui;

import java.io.*;
import javax.swing.*;

import me.footlights.core.Footlights;
import me.footlights.core.UI;


public class SwingUI extends UI
{
	public static SwingUI init(Footlights footlights)
	{
		return new SwingUI(footlights);
	}

	public void handleEvent(Event event) {}

	private SwingUI(Footlights footlights)
	{
		super("Java UI", footlights);

		String name = "Footlights";

		frame = new JFrame(name);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		appList = new ApplicationList(footlights);
		frame.getContentPane().add(appList, java.awt.BorderLayout.LINE_START);

		textArea = new JTextArea();
		textArea.setBorder(BorderFactory.createLoweredBevelBorder());
		textArea.setBackground(java.awt.Color.BLACK);
		textArea.setForeground(java.awt.Color.GRAY);

		JScrollPane scrollPane = new JScrollPane(textArea);
		frame.getContentPane().add(scrollPane, java.awt.BorderLayout.CENTER);

		frame.pack();
		frame.setBounds(0, 0, 800, 600);
		frame.setVisible(true);

		OutputStream outStream = new OutputStream()
		{
			@Override public void write(int b) throws IOException
			{
				textArea.append(new String(new char[] { (char) b }));
				textArea.setCaretPosition(textArea.getDocument().getLength());
			}
		};

		printStream = new PrintStream(outStream);
		System.setOut(printStream);
		System.setErr(printStream);
	}

	@Override public void run() { printStream.println("Swing UI started."); }


	private JFrame frame;
	private ApplicationList appList;
	private JTextArea textArea;
	private PrintStream printStream;
}

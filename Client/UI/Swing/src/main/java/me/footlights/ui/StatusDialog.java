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


public class StatusDialog
{
	public void status(String status)
	{
		label.setText(status);
		System.out.println(status);
	}


	public PrintStream stream() { return printStream; }


	public static StatusDialog instance()
	{
		if(instance == null) instance =
			new StatusDialog("Footlights Status");

		return instance;
	}

	
	protected StatusDialog(String name)
	{
		label = new JLabel();
		textArea = new JTextArea();
		textArea.setBackground(java.awt.Color.BLACK);
		textArea.setForeground(java.awt.Color.GRAY);

		JScrollPane scrollPane = new JScrollPane(textArea);

		frame = new JFrame(name);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(label, java.awt.BorderLayout.NORTH);
		frame.getContentPane().add(scrollPane, java.awt.BorderLayout.CENTER);
		frame.pack();
		frame.setBounds(0, 0, 480, 240);
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

	private static StatusDialog instance;

	private PrintStream printStream;
	private JFrame frame;
	private JLabel label;
	private JTextArea textArea;
}

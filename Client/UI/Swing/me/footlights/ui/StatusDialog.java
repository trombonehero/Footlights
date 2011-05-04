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

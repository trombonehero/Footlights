package me.footlights.ui;



import java.io.*;
import javax.swing.*;

import me.footlights.core.Footlights;
import me.footlights.core.plugin.PluginWrapper;


public class SwingUI extends me.footlights.core.UI
{
	public SwingUI(Footlights footlights)
	{
		super("Java UI", footlights);

		String name = "Footlights";

		frame = new JFrame(name);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		pluginList = new PluginList(footlights);
		frame.getContentPane().add(pluginList, java.awt.BorderLayout.LINE_START);

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


	@Override public void pluginLoaded(PluginWrapper plugin)
	{
		pluginList.pluginLoaded(plugin);
	}

	@Override public void pluginUnloading(PluginWrapper plugin)
	{
		pluginList.pluginUnloading(plugin);
	}


	@Override public void run() { printStream.println("Swing UI started."); }


	private JFrame frame;
	private PluginList pluginList;
	private JTextArea textArea;
	private PrintStream printStream;
}

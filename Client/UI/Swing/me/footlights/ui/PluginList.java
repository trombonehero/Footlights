package me.footlights.ui;


import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import me.footlights.core.Footlights;
import me.footlights.core.plugin.*;


import static me.footlights.core.Log.log;


public class PluginList extends JPanel implements PluginWatcher
{
	public PluginList(Footlights footlights)
	{
		this.footlights = footlights;

		setBorder(BorderFactory.createRaisedBevelBorder());
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(new JLabel("Plugins"));

		plugins = new JPanel();
		plugins.setLayout(new BoxLayout(plugins, BoxLayout.PAGE_AXIS));
		add(plugins);

		refresh();
	}


	public void pluginLoaded(PluginWrapper plugin) { refresh(); }
	public void pluginUnloading(PluginWrapper plugin) { refresh(); }


	void refresh()
	{
		this.plugins.removeAll();

		Collection<PluginWrapper> plugins = footlights.plugins();
		for(final PluginWrapper plugin : plugins)
		{
			JPanel pluginPanel = new JPanel();
			pluginPanel.setBorder(BorderFactory.createRaisedBevelBorder());
			pluginPanel.setLayout(new BoxLayout(pluginPanel, BoxLayout.PAGE_AXIS));

			pluginPanel.add(new JLabel(plugin.wrapped().name()));

			JButton run = new JButton("Run");
			run.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent arg0)
					{
						plugin.run();
						try
						{
							log(plugin.output());
						}
						catch(Throwable t)
						{
							log("Error: " + t.getClass().getName());
							t.printStackTrace();
						}
					}
				});
			pluginPanel.add(run);

			JButton unload = new JButton("Unload");
			unload.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent arg0)
					{
						footlights.unloadPlugin(plugin);
					}
				});
			pluginPanel.add(unload);

			this.plugins.add(pluginPanel);
		}

		updateUI();
	}


	private Footlights footlights;
	private JPanel plugins;

	private static final long serialVersionUID
		= "footlights.ui.PluginList@2010-02-12/1433h".hashCode();
}

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


import java.awt.event.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import me.footlights.core.Footlights;
import me.footlights.core.plugin.*;


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
		for (final PluginWrapper plugin : plugins)
		{
			JPanel pluginPanel = new JPanel();
			pluginPanel.setBorder(BorderFactory.createRaisedBevelBorder());
			pluginPanel.setLayout(new BoxLayout(pluginPanel, BoxLayout.PAGE_AXIS));

			//pluginPanel.add(new JLabel(plugin.wrapped().name()));

			JButton run = new JButton("Run");
			run.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent arg0)
					{
						//plugin.run();
						try
						{
							//log(plugin.output());
						}
						catch(Throwable t)
						{
							log.log(Level.SEVERE, "Uncaught error; + t.getClass().getName()", t);
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


	private static final Logger log = Logger.getLogger(PluginList.class.getName());

	private Footlights footlights;
	private JPanel plugins;

	private static final long serialVersionUID
		= "footlights.ui.PluginList@2010-02-12/1433h".hashCode();
}

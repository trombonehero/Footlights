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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import me.footlights.core.Footlights;
import me.footlights.core.apps.AppWrapper;


public class ApplicationList extends JPanel
{
	public ApplicationList(Footlights footlights)
	{
		this.footlights = footlights;

		setBorder(BorderFactory.createRaisedBevelBorder());
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(new JLabel("Apps"));

		apps = new JPanel();
		apps.setLayout(new BoxLayout(apps, BoxLayout.PAGE_AXIS));
		add(apps);

		refresh();
	}


	public void applicationLoaded(AppWrapper app) { refresh(); }
	public void applicationUnloading(AppWrapper app) { refresh(); }


	void refresh()
	{
		this.apps.removeAll();

		scala.collection.Seq<AppWrapper> apps = footlights.runningApplications();
		for (int i = 0; i < apps.size(); i++)
		{
			final AppWrapper app = apps.apply(i);

			JPanel appPanel = new JPanel();
			appPanel.setBorder(BorderFactory.createRaisedBevelBorder());
			appPanel.setLayout(new BoxLayout(appPanel, BoxLayout.PAGE_AXIS));

			//appPanel.add(new JLabel(app.wrapped().name()));

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
			appPanel.add(run);

			JButton unload = new JButton("Unload");
			unload.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent arg0)
					{
						footlights.unloadApplication(app);
					}
				});
			appPanel.add(unload);

			this.apps.add(appPanel);
		}

		updateUI();
	}


	private static final Logger log = Logger.getLogger(ApplicationList.class.getName());

	private Footlights footlights;
	private JPanel apps;

	private static final long serialVersionUID
		= ("5 Dec 2011 1954h GMT" + ApplicationList.class.getCanonicalName()).hashCode();
}

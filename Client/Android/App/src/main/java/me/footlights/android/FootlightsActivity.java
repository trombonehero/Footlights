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
package me.footlights.android;

import java.util.Map;

import com.google.inject.Inject;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

import me.footlights.core.Core;
import me.footlights.core.Preferences;


public class FootlightsActivity extends RoboActivity
{
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final EventLog log = new EventLog(this);
		eventsList.setAdapter(log.adapter());

		Button[] buttons = { buttonA, buttonB, buttonC, buttonD };
		for (final Button button : buttons)
		{
			button.setOnClickListener(new View.OnClickListener()
			{
				@Override public void onClick(View v)
				{
					log.log("Clicked '" + button.getText() + "'");
				}
			});
		}

		log.log("Created Footlights core: " + core + "\n");

		Preferences prefs = Preferences.create(PreferenceAdapter.wrap(sharedPrefs));
		for (Map.Entry<String,?> entry : prefs.getAll().entrySet())
			log.log(entry.getKey() + ": " + entry.getValue().toString());
	}

	private @Inject Core core;
	private @Inject SharedPreferences sharedPrefs;

	private @InjectView(R.id.buttonA) Button buttonA;
	private @InjectView(R.id.buttonB) Button buttonB;
	private @InjectView(R.id.buttonC) Button buttonC;
	private @InjectView(R.id.buttonD) Button buttonD;

	private @InjectView(R.id.eventsList) ListView eventsList;
}

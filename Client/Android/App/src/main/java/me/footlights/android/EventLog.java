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

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;


/** An event log which displays events and the times they occurred. */
class EventLog
{
	public EventLog(final Context context)
	{
		items = new LinkedList<EventLog.Event>();
		adapter = createAdapter(context);
	}

	public void log(String message)
	{
		items.addFirst(new Event(new Date(), message));
		adapter.notifyDataSetChanged();
	}

	public ListAdapter adapter()
	{
		return adapter;
	}

	private BaseAdapter createAdapter(final Context context)
	{
		return new BaseAdapter()
		{
			@Override
			public int getCount()
			{
				return items.size();
			}

			@Override
			public Object getItem(int position)
			{
				return items.get(position);
			}

			@Override
			public boolean hasStableIds()
			{
				return false;
			}

			@Override
			public long getItemId(int position)
			{
				return 0;
			}

			@Override
			public View getView(int position, View old, ViewGroup parent)
			{
				EventView view;

				if (old instanceof EventView)
					view = (EventView) old;
				else
					view = new EventView(context);

				final Event event = items.get(position);
				view.setTime(event.when);
				view.setMessage(event.what);

				return view;
			}
		};
	}

	private class Event
	{
		public Event(Date time, String message)
		{
			when = time;
			what = message;
		}

		private Date when;
		private String what;
	}

	private class EventView extends LinearLayout
	{
		public EventView(Context context)
		{
			super(context);

			this.setOrientation(VERTICAL);

			when = new TextView(context);
			when.setTextSize(10);
			addView(when, new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

			what = new TextView(context);
			addView(what, new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		}

		public void setTime(Date d)
		{
			when.setText(DateFormat.getDateTimeInstance().format(d));
		}

		public void setMessage(String s)
		{
			what.setText(s);
		}

		private TextView when;
		private TextView what;
	}

	private final LinkedList<Event> items;
	private final BaseAdapter adapter;
}

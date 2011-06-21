package me.footlights.android;

import me.footlights.core.Core;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;


public class FootlightsActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final EventLog log = new EventLog(this);

        ListView eventsList = (ListView) findViewById(R.id.eventsList);
        eventsList.setAdapter(log.adapter());

        int[] buttons = { R.id.buttonA, R.id.buttonB, R.id.buttonC, R.id.buttonD };
        for (int id : buttons)
        {
        	final Button button = (Button) findViewById(id);
        	button.setOnClickListener(
	        	new View.OnClickListener()
		        {
					@Override public void onClick(View v)
					{
						log.log("Clicked button '" + button.getText() + "'");
					}
				});
        }

        me.footlights.core.Log flog = me.footlights.core.Log.instance();
        log.log("Created log: " + flog + "\n");

        Core core = new Core();
        log.log("Created Footlights core: " + core + "\n");
    }
}
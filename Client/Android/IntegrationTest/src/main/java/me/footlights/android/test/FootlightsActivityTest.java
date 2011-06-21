package me.footlights.android.test;

import android.test.ActivityInstrumentationTestCase2;
import me.footlights.android.*;

public class FootlightsActivityTest extends ActivityInstrumentationTestCase2<FootlightsActivity> {

    public FootlightsActivityTest() {
        super(FootlightsActivity.class);
    }

    public void testActivity() {
    	FootlightsActivity activity = getActivity();
        assertNotNull(activity);
    }
}


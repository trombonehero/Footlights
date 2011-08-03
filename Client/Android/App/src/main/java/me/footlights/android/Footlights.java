package me.footlights.android;

import java.util.List;

import com.google.inject.Module;

import roboguice.application.RoboApplication;


public class Footlights extends RoboApplication
{
    protected void addApplicationModules(List<Module> modules)
    {
        modules.add(new GuiceModule());
    }
}

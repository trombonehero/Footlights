package me.footlights.core.data;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;



@RunWith(Suite.class)
@Suite.SuiteClasses(
	{
		LinkTests.class,
		BlockTests.class,
		FileTests.class,

		me.footlights.core.data.store.StoreTests.class
	})

public class DataTests {}


package me.footlights.core.data.store;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses(
		{
			DiskStoreTests.class,
			FetcherTests.class,
			MemoryStoreTests.class
		})

public class StoreTests {}

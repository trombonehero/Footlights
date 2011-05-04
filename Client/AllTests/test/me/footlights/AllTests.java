package me.footlights;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(
		{
			me.footlights.core.CoreTests.class,
			me.footlights.store.blockstore.BlockStoreTests.class,
		})

public class AllTests
{
}

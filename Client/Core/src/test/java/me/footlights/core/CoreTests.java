package me.footlights.core;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses(
		{
			me.footlights.core.crypto.FingerprintTest.class,
			me.footlights.core.crypto.KeychainTests.class,
			me.footlights.core.crypto.SecretKeyTest.class,
			me.footlights.core.data.DataTests.class,
			me.footlights.core.plugin.PluginTests.class
		})

public class CoreTests {}

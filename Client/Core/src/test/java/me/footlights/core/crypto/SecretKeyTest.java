package me.footlights.core.crypto;

import org.junit.Test;
import static org.junit.Assert.*;


/** Tests {@see SecretKey}. */
public class SecretKeyTest
{
	@Test public void testGeneration() throws Throwable
	{
		SecretKey k = SecretKey.newGenerator().generate();

		assertTrue(k.keySpec.getEncoded().length > 0);
	}
}

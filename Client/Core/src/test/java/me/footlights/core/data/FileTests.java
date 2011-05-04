package me.footlights.core.data;

import me.footlights.core.data.File;

import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class FileTests
{
	@Test public void anonymousFile() throws Throwable
	{
		File f = new File();

		assertEquals(0, f.content().size());
	}
}

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
package me.footlights.api.ajax;

import me.footlights.api.ajax.JSON;

import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;


public class JSONTest
{
	@Test public void testPrimitives()
	{
		assertEquals("{ \"foo\": 42, \"bar\": 3.1415926, \"baz\": \"Hello, world!\" }",
			JSON.newBuilder()
				.put("foo", 42)
				.put("bar", 3.1415926)
				.put("baz", "Hello, world!")
				.build()
				.toString()
		);
	}

	@Test public void testArrays()
	{
		assertEquals("{ \"array\": [ 1, 2, 3 ] }",
			JSON.newBuilder()
				.putInts("array", Lists.newArrayList(1, 2, 3))
				.build()
				.toString()
		);
	}

	@Test public void testSubJSON()
	{
		assertEquals("{ \"sub\": { \"foo\": \"bar\" } }",
			JSON.newBuilder()
				.put("sub", JSON.newBuilder().put("foo", "bar").build())
				.build()
				.toString()
		);
	}

	@Test public void testComplex()
	{
		assertEquals("{ \"foo\": 42, \"bar\": [ 1.0, 2.0 ], \"baz\": { \"hello\": \"world\" } }",
			JSON.newBuilder()
				.put("foo", 42)
				.putDoubles("bar", Lists.newArrayList(1.0, 2.0))
				.put("baz", JSON.newBuilder().put("hello", "world").build())
				.build()
				.toString()
		);
	}
}

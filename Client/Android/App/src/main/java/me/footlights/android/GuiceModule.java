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
package me.footlights.android;

import com.google.inject.AbstractModule;

import me.footlights.core.Kernel;


public class GuiceModule extends AbstractModule
{
	@Override protected void configure()
	{
		ClassLoader loader = GuiceModule.class.getClassLoader();

		Kernel kernel = null;
		try { kernel = Kernel.init(loader); }
		catch (Exception e)
		{
			System.err.println("Error creating kernel: " + e);
			e.printStackTrace(System.err);
		}

		bind(Kernel.class).toInstance(kernel);
	}
}

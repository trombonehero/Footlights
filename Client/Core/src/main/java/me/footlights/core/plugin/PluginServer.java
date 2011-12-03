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
package me.footlights.core.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.footlights.core.Footlights;
import me.footlights.plugin.File;
import me.footlights.plugin.KernelInterface;


/** Services requests from plugins */
public class PluginServer implements KernelInterface
{
	public PluginServer(Footlights kernel)
	{
		this.kernel = kernel;
	}


	// KernelInterface implementation
	@Override public File save(ByteBuffer data) throws IOException
	{
		return kernel.save(data);
	}

	@Override public File open(String name) throws IOException { return kernel.open(name); }
	@Override public File openLocalFile() throws IOException { return kernel.openLocalFile(); }


	/** The system core */
	private Footlights kernel;
}

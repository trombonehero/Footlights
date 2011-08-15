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

import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;

import me.footlights.plugin.KernelInterface;
import me.footlights.plugin.Plugin;


class TrivialPlugin implements Plugin
{
	@Override public void run(KernelInterface kernel, Logger log)
	{
		log.info(OUTPUT);
	}

	@VisibleForTesting static String OUTPUT = "The trivial demo plugin is now running";
}

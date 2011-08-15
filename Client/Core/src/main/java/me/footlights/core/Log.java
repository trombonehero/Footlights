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
package me.footlights.core;

import java.io.PrintStream;


public class Log
{
	public static void log(String line)
	{
		instance().logStatus(line);
	}


	public void logStatus(String line)
	{
		System.out.println(line);
	}


	public static Log instance()
	{
		if(instance == null) instance = new Log();
		return instance;
	}

	public void setStream(PrintStream stream) { outputStream = stream; }
	public PrintStream stream() { return outputStream; }

	protected Log()
	{
		outputStream = System.out;

//		footlights.core.ui.StatusDialog.status("Redirecting stdout...");
//		outputStream = footlights.core.ui.StatusDialog.instance().stream();	
	}

	private static Log instance;
	private PrintStream outputStream;
}

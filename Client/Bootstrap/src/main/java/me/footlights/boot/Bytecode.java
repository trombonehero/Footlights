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
package me.footlights.boot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;


/** Holds bytecode from a Java class file */
class Bytecode
{
	/** Read bytecode from a class file using a classpath and class name. */
	public static Bytecode readFile(URL classpath, String className)
			throws ClassNotFoundException, MalformedURLException
	{
		// Construct a local path to the class file.
		StringBuffer path = new StringBuffer();
		path.append(classpath.toExternalForm().replaceFirst("^file:", ""));

		for (String subdir : className.split("\\."))
		{
			path.append(File.separatorChar);
			path.append(subdir);
		}

		path.append(".class");

		// Read in the class.
		try
		{
			File file = new File(path.toString());
			FileInputStream in = new FileInputStream(file);
			byte[] raw = new byte[(int) file.length()];

			for (int offset = 0; offset < raw.length; )
				offset += in.read(raw, offset, raw.length - offset);

			in.close();

			Bytecode bytecode = new Bytecode();
			bytecode.raw = raw;
			bytecode.source = new CodeSource(classpath, new CodeSigner[0]);
			return bytecode;
		}
		catch(IOException e)
		{
			throw new ClassNotFoundException(e.getLocalizedMessage());
		}
	}


	public byte[] raw;
	public CodeSource source;
}

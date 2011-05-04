package me.footlights.boot;


import java.io.*;
import java.net.*;
import java.security.*;
import java.util.List;


/** Loads "core" code (footlights.core.*, footlights.ui.*) from a known source */
public class FootlightsClassLoader extends URLClassLoader
{
	/** Constructor */
	public FootlightsClassLoader(List<URL> classpath) throws MalformedURLException
	{
		super(classpath.toArray(new URL[0]));

		corePermissions = new Permissions();
		corePermissions.add(new AllPermission());
	}



	/** Find a core Footlights class */
	@Override protected synchronized Class<?> findClass(String name)
		throws ClassNotFoundException
	{
		if(!name.startsWith("me.footlights"))
			throw new ClassNotFoundException();

		Bytecode bytecode = readBytecode(name);

		ProtectionDomain domain
			= new ProtectionDomain(bytecode.source, corePermissions);

		Class<?> c = defineClass(name, bytecode.raw,
		                         0, bytecode.raw.length, domain);

		return c;
	}


	@Override public synchronized URL findResource(String name)
	{
		for(URL url : getURLs())
		{
			try
			{
				URL bigURL = new URL(url.toString() + "/" + name);
				if(new File(bigURL.getFile()).exists()) return bigURL;

				int i = 0;
				i++;
			}
			catch(MalformedURLException e) { throw new Error(e); }
		}
		return super.findResource(name);
	}



	/** Read bytecode for a core Footlights class. */
	private Bytecode readBytecode(String className)
		throws ClassNotFoundException
	{
		for(URL url : getURLs())
			try
			{
				if(url.toExternalForm().matches(".*\\.jar$"))
					return new JARLoader(url).readBytecode(className);

				else
					return readClassFile(url, className);
			}
			catch(ClassNotFoundException e) {}
			catch(IOException e) {}

		throw new ClassNotFoundException();
	}



	/** Read bytecode from a class file using a classpath and class name. */
	private Bytecode readClassFile(URL url, String className)
			throws ClassNotFoundException
	{
		// construct the path of the class file
		String path = url.toExternalForm();
		if(!path.startsWith("file:"))
			throw new ClassNotFoundException("The class path URI " + path
				+ " does not start with 'file:'");
		path = path.replaceFirst("file:", "");
		
		String[] subdirs = className.split("\\.");
		for(int i = 0; i < (subdirs.length - 1); i++)
			path = path + File.separatorChar + subdirs[i];


		// open the file and read it
		String classFileName = subdirs[subdirs.length - 1] + ".class";
		File file = new File(path + File.separatorChar + classFileName);

		try
		{
			InputStream in = new FileInputStream(file);
			byte[] raw = new byte[(int) file.length()];

			for(int offset = 0; offset < raw.length; )
				offset += in.read(raw, offset, raw.length - offset);

			in.close();

			Bytecode bytecode = new Bytecode();
			bytecode.raw = raw;
			bytecode.source = new CodeSource(url, new CodeSigner[0]);
			return bytecode;
		}
		catch(IOException e)
		{
			throw new ClassNotFoundException(e.getLocalizedMessage());
		}
	}


	/** Cached permissions given to core classes */
	private Permissions corePermissions;
}

package problem10199909;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


public class CrashMaker
{
	public static final void main(String[] args) throws Exception
	{
		System.out.println("This is a demonstration of Problem 10199909.");
		System.out.println("");

		if (args.length != 2)
			err("Usage:\n  crashmaker.jar <A>.jar <B>.jar");

		File a = new File(args[0]);
		File b = new File(args[1]);
		if (!a.exists()) err("Non-existent file: " + a);
		if (!b.exists()) err("Non-existent file: " + b);

		File target = File.createTempFile("crash-demo", ".jar");
		URL jar = new URL("jar:" + target.toURI() + "!/");
		System.out.println("Using target '" + jar + "'\n");

		// Copy one JAR to the target location and load a resource.
		final String resourceName = PACKAGE + "/Demo.class";
		cp(a, target);
		loadFrom(jar, resourceName);

		// Ok, that worked. Try again with the other JAR file...
		cp(b, target);
		loadFrom(jar, resourceName);
	}


	private static final void err(String message)
	{
		System.err.println(message);
		System.exit(1);
	}

	private static final void cp(File from, File to) throws IOException
	{
		System.out.println("Copying '" + from + "' to '" + to + "'");
		Runtime.getRuntime().exec(new String[] { "cp", from.toString(), to.toString() });
	}

	private static final void loadFrom(URL jar, String name) throws IOException
	{
		System.out.print("Reading '" + name + "' from JAR... ");

		String externalUrl = jar.toExternalForm();

		StringBuilder sb = new StringBuilder();
		sb.append(externalUrl);
		if (!externalUrl.endsWith("/")) sb.append('/');
		sb.append(name);

		InputStream stream = new URL(sb.toString()).openStream();

		int bytes = stream.read(new byte[2048]);
		System.out.println(bytes + " B");

		stream.close();
	}

	private static final String PACKAGE = CrashMaker.class.getPackage().getName();
}

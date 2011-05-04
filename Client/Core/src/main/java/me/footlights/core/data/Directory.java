package me.footlights.core.data;

import java.util.HashMap;
import java.util.Map;


/** A directory of files */
public class Directory extends File
{
	/** I/O modes for opening a directory's files */
	enum Mode { READ, WRITE };


	/** Constructor */
	public Directory()
	{
		files = new HashMap<String,File>();
	}


	/** Retrieve a file from the directory */
	public File open(String name, Mode mode) 
	{
		File f = files.get(name);

		if((mode == Mode.WRITE) && (f.isWritable())) return f.readOnly();
		else return f;
	}


	/** Files contained in the directory */
	private Map<String,File> files;
}

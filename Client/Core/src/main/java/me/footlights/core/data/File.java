package me.footlights.core.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import com.google.common.collect.Lists;


/** A file */
public class File
{
	/** Default constructor; produces an anonymous file */
	public File()
	{
		plaintext = new LinkedList<Block>();
	}


	/** Do we have the necessary authority to publish this file? */
	public boolean isWritable() { return (publisher != null); }


	/** Make a read-only copy of this file */
	public File readOnly()
	{
		File f = new File();
		f.plaintext = plaintext;
		f.publisher = null;

		return f;
	}


	/**
	 * The contents of the file.
	 *  
	 * @throws IOException    on I/O errors such as network failures
	 */
	public List<ByteBuffer> content() throws IOException
	{
		List<ByteBuffer> content = Lists.newLinkedList();
		for(Block b : plaintext) content.add(b.content());

		return content;
	}


	private List<Block> plaintext;
	private List<Block> encrypted;

	/** We'll figure out how this actually works later */
	Object publisher;
}

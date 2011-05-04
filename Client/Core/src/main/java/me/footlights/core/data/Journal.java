package me.footlights.core.data;

import java.util.LinkedList;
import java.util.List;


public class Journal
{
	public Journal()
	{
		entries = new LinkedList<String>();
	}


	public void append(String entry) { entries.add(entry); }
	public List<String> entries() { return entries; }


	private List<String> entries;
}

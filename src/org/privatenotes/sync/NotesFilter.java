package org.privatenotes.sync;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Simple filename filter that grabs files ending with .note
 */
public class NotesFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return (name.endsWith(".note"));
	}
}
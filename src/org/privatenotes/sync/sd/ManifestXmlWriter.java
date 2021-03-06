/*
 * PrivateNotes
 * 
 * Copyright 2011 Paul Klingelhuber <paul.klingelhuber@students.fh-hagenberg.at>
 * 
 * This file is part of PrivateNotes.
 * 
 * Tomdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Tomdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Tomdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.privatenotes.sync.sd;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.privatenotes.Note;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

/**
 * Class that supports generation of Manifest files as used by Tomboy for local file
 * sync or webdav sync.
 *
 */
public class ManifestXmlWriter {
	protected static final String NODE_ROOT = "sync";
	protected static final String NODE_NOTE = "note";
	protected static final String ATT_ROOT_MAXREV = "revision";
	protected static final String ATT_ROOT_SERVERID = "server-id";
	protected static final String ATT_NOTE_ID = "id";
	protected static final String ATT_NOTE_REV = "rev";
	protected static final String NS = null;
	
	/**
	 * serialize a set of notes to a manifest file which contents are returned as a string
	 * 
	 * @param notes all notes that should be serialized
	 * @param serverId the server id that should be used
	 * @param newRevision the revision number that should be set for all notes that have been changed (also this is maxRev)
	 * @param untouchedNotes list of the notes that have not been changed
	 * @param oldRevision value that should be used as an old revision if a revision cannot be found in the revs map
	 * @param revs the revisions of the notes. if possible this should include ALL notes that are supplied via the lists
	 * @return the serialized version of the manifest
	 * @throws Exception
	 */
	public String serialize(List<Note> notes, String serverId, long newRevision, List<String> untouchedNotes, long oldRevision, Map<String, Integer> revs) throws Exception {		
		XmlSerializer ser = Xml.newSerializer();
		StringWriter sw = new StringWriter();
		ser.setOutput(sw);
		ser.startDocument("utf-8", null);
		ser.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		
		ser.startTag(NS, NODE_ROOT);
		if (newRevision > 0)
			ser.attribute(NS, ATT_ROOT_MAXREV, "" + newRevision);
		else
			ser.attribute(NS, ATT_ROOT_MAXREV, String.valueOf(getMaxRev(notes)));
		ser.attribute(NS, ATT_ROOT_SERVERID, serverId);

		for (Note n : notes) {
			writeNote(ser, n, newRevision);
		}
		
		for (String id : untouchedNotes) {
			// check if we have a revision from before
			Integer oldRev = revs.get(id);
			// if not take default "old revision" number
			writeNote(ser, id, (oldRev != null?oldRev.longValue():oldRevision));
		}
		
		afterNoteVersionNodes(ser);
		
		ser.endTag(NS, NODE_ROOT);
		
		ser.flush();
		return sw.getBuffer().toString();
		
	}
	
	/**
	 * can be overridden if you want to put sth after the main note-version elements
	 * @param ser
	 */
	protected void afterNoteVersionNodes(XmlSerializer ser) throws IOException {
		
	}
	
	/**
	 * writes the information of one note into the xml structure
	 * @param ser
	 * @param note note object (only guid of this object is taken)
	 * @param rev
	 * @throws Exception
	 */
	private void writeNote(XmlSerializer ser, Note note, long rev) throws Exception {
		ser.startTag(NS, NODE_NOTE);
		ser.attribute(NS, ATT_NOTE_ID, note.getGuid().toString());
		if (rev > 0)
			ser.attribute(NS, ATT_NOTE_REV, "" + rev);
		else
			ser.attribute(NS, ATT_NOTE_REV, "" + note.getLastSyncRevision());
		ser.endTag(NS, NODE_NOTE);
	}
	
	/**
	 * writes the information of one note into the xml structure
	 * @param ser
	 * @param guid the note universal id
	 * @param rev
	 * @throws Exception
	 */
	private void writeNote(XmlSerializer ser, String guid, long rev) throws Exception {
		ser.startTag(NS, NODE_NOTE);
		ser.attribute(NS, ATT_NOTE_ID, guid);
		ser.attribute(NS, ATT_NOTE_REV, "" + rev);
		ser.endTag(NS, NODE_NOTE);
	}
	
	/**
	 * gets the maximum revision of a list of notes
	 * @param notes
	 * @return
	 */
	private long getMaxRev(List<Note> notes) {
		long biggestRev = -1;
		for (Note note : notes) {
			biggestRev = (biggestRev<note.getLastSyncRevision())?note.getLastSyncRevision():biggestRev;
		}
		return biggestRev;
	}
	
}

/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2011 Paul Klingelhuber <paul.klingelhuber@students.fh-hagenberg.at>
 * 
 * This file is part of Tomdroid.
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
import java.util.List;
import java.util.Map;

import org.privatenotes.Note;
import org.xmlpull.v1.XmlSerializer;

/**
 * Class that supports generation of Manifest files for shares, which is a new version
 * of manifests that was introduced by the PrivateNotes project.
 *
 */
public class SharedManifestXmlWriter extends ManifestXmlWriter {
	
	private List<String> sharedWith = null;
	
	/**
	 * serialize shares and share-partners to such an xml
	 * 
	 * @param notes
	 * @param serverId
	 * @param newRevision
	 * @param untouchedNotes
	 * @param oldRevision
	 * @param revs
	 * @param sharedWith a list of share-partners
	 * @return the serialized version of the shared manifest filled with the data provided by the parameters
	 * @throws Exception
	 */
	public String serialize(List<Note> notes, String serverId, long newRevision, List<String> untouchedNotes, long oldRevision, Map<String, Integer> revs, List<String> sharedWith) throws Exception {
		this.sharedWith = sharedWith;
		return super.serialize(notes, serverId, newRevision, untouchedNotes, oldRevision, revs);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void afterNoteVersionNodes(XmlSerializer ser) throws IOException {
		ser.startTag(NS, SharedManifestHandler.NODE_SHARED);
		for (String with : sharedWith) {
			ser.startTag(NS, SharedManifestHandler.NODE_WITH);
			ser.attribute(NS, SharedManifestHandler.ATT_WITH_PARTNER, with);
			ser.endTag(NS, SharedManifestHandler.NODE_WITH);
		}
		ser.endTag(NS, SharedManifestHandler.NODE_SHARED);
	}
	
}

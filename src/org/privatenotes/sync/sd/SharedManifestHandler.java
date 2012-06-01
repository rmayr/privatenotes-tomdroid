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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.util.TimeFormatException;

/**
 * Provides capabilities to read information from a shared manifest. This is a new type of manifest
 * introduced by the PrivateNotes project to hold information about the share-partners.
 *
 */
public class SharedManifestHandler extends ManifestHandler {
	private static final String TAG = "SharedManifestHandler";
	
	private List<String> noteIds = null;
	private List<String> sharedWith = null;
	private boolean xmlInShared = false;
	
	/**
	 * construcotr
	 * @param versionMap version map, into this map the handler will put the versions he reads out
	 */
	public SharedManifestHandler(Map<String, Integer> versionMap) {
		super(versionMap);
		noteIds = new ArrayList<String>();
		sharedWith = new ArrayList<String>();
	}
	
	/**
	 * get all note ids that are shared in this shared manifest
	 * @return
	 */
	public List<String> getNoteIds() {
		return noteIds;
	}

	/**
	 * get all participants of this share
	 * @return
	 */
	public List<String> getSharedWith() {
		return sharedWith;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startElement(String uri, String localName, String name,	Attributes attributes) throws SAXException {
		super.startElement(uri, localName, name, attributes);
		if (NODE_NOTE.equals(localName)) {
			String id = attributes.getValue(ATT_NOTE_ID);
			String rev = attributes.getValue(ATT_NOTE_REV);
			if (id != null && rev != null) {
				noteIds.add(id);
			}
		} else if (NODE_SHARED.equals(localName)) {
			xmlInShared = true;
		} else if (xmlInShared && NODE_WITH.equals(localName)) {
			String partner = attributes.getValue(ATT_WITH_PARTNER);
			if (partner != null) {
				sharedWith.add(partner);
			}
		}
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException, TimeFormatException {
		if (NODE_SHARED.equals(localName)) {
			xmlInShared = false;
		}
	}
	
	public static final String NODE_SHARED = "shared";
	public static final String NODE_WITH = "with";
	public static final String ATT_WITH_PARTNER = "partner";
	
}

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

import java.security.InvalidParameterException;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import android.util.TimeFormatException;

/**
 * Provides capabilities to read information from a Tomboy manifest file which holds
 * information about the notes that are part of a sync location and their revisions.
 *
 */
public class ManifestHandler extends DefaultHandler {
	private static final String TAG = "ManifestHandler";
	
	private Map<String, Integer> versions;
	private String serverId = "";
	
	public String getServerId() {
		return serverId;
	}

	/**
	 * construct a manifestHandler object with a map into which
	 * it will put the information available in the manifest
	 * @param versionMap into this map the handler will write the note GUID and the version numabers
	 */
	public ManifestHandler(Map<String, Integer> versionMap) {
		if (versionMap == null)
			throw new InvalidParameterException();
		
		this.versions = versionMap;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startElement(String uri, String localName, String name,	Attributes attributes) throws SAXException {
		
		// TODO validate top-level tag for tomboy notes and throw exception if its the wrong version number (maybe offer to parse also?)		
		if (NODE_NOTE.equals(localName)) {
			String id = attributes.getValue(ATT_NOTE_ID);
			String rev = attributes.getValue(ATT_NOTE_REV);
			if (id != null && rev != null) {
				int irev = -1;
				try {
					irev = Integer.valueOf(rev);
					versions.put(id, irev);
				}catch (NumberFormatException e) {
					Log.w(TAG, "invalid rev number " + rev);
				}
			} else {
				Log.w(TAG, "note-node has no id or rev in manifest.xml");
			}
		}
		else if (NODE_ROOT.equals(localName)) {
			this.serverId = attributes.getValue(ATT_ROOT_SERVERID);
		}
		
		// all others will be ignored
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException, TimeFormatException {

		// i guess we don't really need it...
	}
	
	public static final String NODE_NOTE = "note";
	public static final String NODE_ROOT = "sync";
	public static final String ATT_ROOT_MAXREV = "revision";
	public static final String ATT_ROOT_SERVERID = "server-id";
	public static final String ATT_NOTE_ID = "id";
	public static final String ATT_NOTE_REV = "rev";
	
}

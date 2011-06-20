package org.tomdroid.sync.sd;

import java.security.InvalidParameterException;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import android.util.TimeFormatException;

public class ManifestHandler extends DefaultHandler {
	private static final String TAG = "ManifestHandler";
	
	private Map<String, Integer> versions;
	
	public ManifestHandler(Map<String, Integer> versionMap) {
		if (versionMap == null)
			throw new InvalidParameterException();
		
		this.versions = versionMap;
	}
	
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
		} // all others will be ignored
		
	}

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

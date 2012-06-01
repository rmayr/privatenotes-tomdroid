/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2011, 2012 Paul Klingelhuber <paul.klingelhuber@students.fh-hagenberg.at>
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
package org.privatenotes.xml;

import java.io.StringWriter;

import org.privatenotes.Note;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

/**
 * This class is a xml serializer for Notes in the native tomboy format.
 * This format is used when doing file sync, webdav sync and also when storing
 * the notes locally on the pc this format is used.
 *
 */
public class NoteXmlSerializer {
	private static final String NODE_ROOT = "note";
	private static final String NODE_TITLE = "title";
	private static final String NODE_LASTCHANGE = "last-change-date";
	private static final String NODE_CONTENT = "note-content";
	private static final String NS = null;
	private static final String NS_XML = "http://www.w3.org/XML/1998/namespace";
	
	/**
	 * Serializes a note to a string
	 * @param note the note object
	 * @return string representation of the note. this should typically be saved
	 * inside a file with this filename pattern: [NOTE-GUID].note
	 * @throws Exception
	 */
	public String serialize(Note note) throws Exception {
		XmlSerializer ser = Xml.newSerializer();
		StringWriter sw = new StringWriter();
		ser.setOutput(sw);
		ser.startDocument("utf-8", null);
		ser.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		ser.setPrefix("size", "http://beatniksoftware.com/tomboy/size");
		ser.setPrefix("link", "http://beatniksoftware.com/tomboy/link");
		ser.startTag(NS, NODE_ROOT);
		ser.attribute(NS, "version", "0.3");
		// we need to declare this namespace via attribute, because overwriting the
		// xmlns namespace IS AN ERROR! you are technically not allowed to do this
		// but since tomboy requires this, we fix it with this hack, because we are
		// such cool basterds  \m/ -_- \m/
		ser.attribute(NS, "xmlns", "http://beatniksoftware.com/tomboy");
		{
			ser.startTag(NS, NODE_TITLE);
			ser.text(note.getTitle());
			ser.endTag(NS, NODE_TITLE);
		}
		{
			ser.startTag(NS, "text");
			// NOTE: i know using namespace like that is kind of a hack, but it didn't work when setting
			// it as the real namespace for me
			//ser.attribute(XML_NS, "xml:space", "preserve");
			ser.attribute(NS_XML, "space", "preserve");
			{
				String xmlContent = note.getXmlContent();
				boolean addContentNode = false;
				if (!xmlContent.startsWith("<" + NODE_CONTENT)) {
					addContentNode = true;
					// NOTE: as it turns out the "note-content" tag is included in the xml text
					ser.startTag(NS, NODE_CONTENT);
					ser.attribute(NS, "version", "1.0");
				}
				
				// NOTE: this is dirty, but we have to do it that way
				// because we cannot write raw xml via the xmlSerializer
				ser.flush();
				sw.write(xmlContent);
				
				if (addContentNode) {
					ser.endTag(NS, NODE_CONTENT);
				}
			}
			ser.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", false);
			ser.endTag(NS, "text");
			ser.flush();
			ser.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		}
		
		writeMetdata(ser, note);
		
		ser.endTag(NS, NODE_ROOT);
		
		ser.flush();
		return sw.getBuffer().toString();
		
	}
	
	/**
	 * writes only the metadata of a note into the xml serializer
	 * @param ser the xml serializer object
	 * @param note the note object
	 * @throws Exception
	 */
	private void writeMetdata(XmlSerializer ser, Note note) throws Exception {
		ser.startTag(NS, NODE_LASTCHANGE);
		// time formatted tomboy-style, seems to be rfc 3339
		// like this: 2011-04-13T22:09:34.8982960+02:00
		ser.text(note.getLastChangeDate().format3339(false));
		ser.endTag(NS, NODE_LASTCHANGE);
	}
}

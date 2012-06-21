/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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
package org.privatenotes;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.privatenotes.util.NoteContentBuilder;
import org.privatenotes.util.XmlUtils;

import android.database.Cursor;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

public class Note implements Cloneable {

	// Static references to fields (used in Bundles, ContentResolvers, etc.)
	public static final String ID = "_id";
	public static final String GUID = "guid";
	public static final String TITLE = "title";
	public static final String MODIFIED_DATE = "modified_date";
	public static final String IS_SYNCED = "is_synced";
	public static final String URL = "url";
	public static final String FILE = "file";
	public static final String TAGS = "tags";
	public static final String NOTE_CONTENT = "content";
	public final static String ENCRYPTED = "encrypted";
	public final static String ENCRYPTED_ALGORITHM = "encryptedAlgorithm";
	
	// Logging info
	private static final String TAG = "Note";
	
	// Notes constants
	// TODO this is a weird yellow that was usable for the android emulator, I must confirm this for real usage
	public static final int NOTE_HIGHLIGHT_COLOR = 0xFFFFFF77;
	public static final String NOTE_MONOSPACE_TYPEFACE = "monospace";
	public static final float NOTE_SIZE_SMALL_FACTOR = 1.0f;
	public static final float NOTE_SIZE_LARGE_FACTOR = 1.5f;
	public static final float NOTE_SIZE_HUGE_FACTOR = 1.8f;
	
	// Members
	private SpannableStringBuilder noteContent;
	private String xmlContent;
	private String url;
	private String fileName;
	private String title;
	private String tags;
	
	private boolean encrypted = false;
	private String encryptedAlgorithm; 
	
	private Time lastChangeDate;
	private int dbId;
	private UUID guid;
	// TODO before guid were of the UUID object type, now they are simple strings 
	// but at some point we probably need to validate their uniqueness (per note collection or universe-wide?) 
	private long lastSyncRevision;
	private boolean isSynced = true;

	
	// Date converter pattern (remove extra sub milliseconds from datetime string)
	// ex: will strip 3020 in 2010-01-23T12:07:38.7743020-05:00
	private static final Pattern dateCleaner = Pattern.compile(
			"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})" +	// matches: 2010-01-23T12:07:38.774
			".+" + 														// matches what we are getting rid of
			"([-\\+]\\d{2}:\\d{2})");									// matches timezone (-xx:xx or +xx:xx)
	
	public Note() {
		tags = new String();
		setTitle("no tilte");
		setGuid(UUID.randomUUID());
		lastSyncRevision = 0;
		changeXmlContent("no content");
		setTags("");
	}
	
	public Note(JSONObject json) {
		
		// These methods return an empty string if the key is not found
		setTitle(XmlUtils.unescape(json.optString("title")));
		setGuid(json.optString("guid"));
		setLastChangeDate(json.optString("last-change-date"));
		lastSyncRevision = json.optInt("last-sync-revision", -1);
		setXmlContent(json.optString("note-content"));
		JSONArray jtags = json.optJSONArray("tags");
		String tag;
		tags = new String();
		if (jtags != null) {
			for (int i = 0; i < jtags.length(); i++ ) {
				tag = jtags.optString(i);
				tags += tag + ",";
			}
		}
		
		// encryption/decryption
		if(json.optString("encrypted")!=null)
		{
			//<encrypted>
			setEncrypted(json.optString("encrypted").toLowerCase().trim().equals("true"));
		}
		//<encryptedAlgorithm>
		setEncryptedAlgorithm(json.optString("encryptedAlgorithm"));
	}
  
	public Note(Cursor cursor) {
		String content = cursor.getString(cursor.getColumnIndexOrThrow(Note.NOTE_CONTENT));
		setXmlContent(content);

		String title = cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE));
		setTitle(title);

		String lastChangeDate = cursor.getString(cursor.getColumnIndexOrThrow(Note.MODIFIED_DATE));
		setLastChangeDate(lastChangeDate);

		setGuid(cursor.getString(cursor.getColumnIndexOrThrow(Note.GUID)));

		setTags(cursor.getString(cursor.getColumnIndexOrThrow(Note.TAGS)));
		
		setEncrypted(cursor.getString(cursor.getColumnIndexOrThrow(Note.ENCRYPTED)).trim().equalsIgnoreCase("true"));
		setEncryptedAlgorithm(cursor.getString(cursor.getColumnIndexOrThrow(Note.ENCRYPTED_ALGORITHM)));
		setFileName(cursor.getString(cursor.getColumnIndexOrThrow(Note.FILE)));

		int synced = cursor.getInt(cursor.getColumnIndexOrThrow(Note.IS_SYNCED));
		isSynced(synced == 1 ? true : false);
	}

	/**
	 * Weather the note is in sync with the server or not. Default is 'true'.
	 */
	public boolean isSynced() {
		return isSynced;
	}

	public void isSynced(boolean flag) {
		isSynced = flag;
	}
	
	public void setEncryptedAlgorithm(String _encryptedAlgorithm)
	{
		encryptedAlgorithm = _encryptedAlgorithm;
		
	}

	public void setEncrypted(boolean _encrypted)
	{
		encrypted = _encrypted;
	}

	public boolean isEncrypted()
	{
		return encrypted;
	}

	public String getEncryptedAlgorithm()
	{
		return encryptedAlgorithm;
	}

	public String getTags() {
		return tags;
	}
  
  public void setTags(String tags) {
		this.tags= tags;
	}

	public String getUrl() {
		return url;
	}
  
	public long getLastSyncRevision() {
		return lastSyncRevision;
	}

	public void setLastSyncRevision(long revision) {
		 lastSyncRevision = revision;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Time getLastChangeDate() {
		return lastChangeDate;
	}
	
	
	
	public void setLastChangeDate(Time lastChangeDate) {
		this.lastChangeDate = lastChangeDate;
    lastChangeDate.switchTimezone(Time.TIMEZONE_UTC);
	}
	
	public void setLastChangeDate(String lastChangeDateStr) throws TimeFormatException {
		
		// regexp out the sub-milliseconds from tomboy's datetime format
		// Normal RFC 3339 format: 2008-10-13T16:00:00.000-07:00
		// Tomboy's (C# library) format: 2010-01-23T12:07:38.7743020-05:00
		Matcher m = dateCleaner.matcher(lastChangeDateStr);
		if (m.find()) {
			Log.d(TAG, "I had to clean out extra sub-milliseconds from the date");
			lastChangeDateStr = m.group(1)+m.group(2);
			Log.v(TAG, "new date: "+lastChangeDateStr);
      lastChangeDateStr = m.group(1) + m.group(2);
      Log.v(TAG, "new date: " + lastChangeDateStr);
		}
		
		lastChangeDate = new Time();
		lastChangeDate.parse3339(lastChangeDateStr);
		lastChangeDate.switchTimezone(Time.TIMEZONE_UTC);
	}	

	public int getDbId() {
		return dbId;
	}

	public void setDbId(int id) {
		this.dbId = id;
	}
	
	public UUID getGuid() {
		return guid;
	}
	
	public void setGuid(String guid) {
		this.guid = UUID.fromString(fixUUID(guid));
	}
  
	public void setGuid(UUID guid) {
		this.guid = guid;
	}

	// TODO: should this handler passed around evolve into an observer pattern?
	public SpannableStringBuilder getNoteContent(Handler handler) {
		
		// TODO not sure this is the right place to do this
		noteContent = new NoteContentBuilder().setCaller(handler).setInputSource(getXmlContent())
				.build();
		return noteContent;
	}
	
	public String getXmlContent() {
		return xmlContent;
	}
	
	public void setXmlContent(String xmlContent) {
		this.xmlContent = xmlContent;
	}
  
	/**
	 * Updates the content and sets last-change-date to now and flags the note as "not in sync with server".
	 */
	public void changeXmlContent(String xmlContent) {
		this.xmlContent = xmlContent;
		Time time = new Time();
		time.setToNow();
		setLastChangeDate(time);
		isSynced(false);
	}

	public JSONObject toJsonWithoutContent() throws JSONException {
		JSONObject json = toJson();
		json.remove("note-content");
		return json;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Note))
			return false;

		Note note = (Note) obj;
		if (note.getGuid().equals(getGuid())
				&& note.getLastChangeDate().equals(getLastChangeDate())
				&& note.getTitle().equals(getTitle()))
			return true;

		return false;
	}

	public JSONObject toJson() throws JSONException {
		return new JSONObject("{'guid':'" + getGuid() + "', 'title':'" + getTitle()
				+ "', 'note-content':'" + getJsonPreparedXmlContent() + "', 'last-change-date':'"
				+ getLastChangeDate().format3339(false) + "', 'note-content-version':0.1}");
	}
	
	/**
	 * makes sure we have a valid uuid, sometimes they arrive without the
	 * dashes (seen in Google play crash reports)
	 * @param uuid
	 * @return
	 */
	public static String fixUUID(String uuid) {
		if (uuid.contains("-")) {
			return uuid;
		} else {
			Log.d(TAG, "fixing guid " + uuid);
			StringBuilder sb = new StringBuilder(38);
			sb.append(uuid.substring(0, 8));
			sb.append("-");
			sb.append(uuid.substring(8, 12));
			sb.append("-");
			sb.append(uuid.substring(12, 16));
			sb.append("-");
			sb.append(uuid.substring(16, 20));
			sb.append("-");
			sb.append(uuid.substring(20));
			return sb.toString();
		}
	}

	private String getJsonPreparedXmlContent() {
		return getXmlContent().replace("\n", "\\n")
				              .replace("\"", "\\\"")
				              .replace("'", "\\'")
				              .replace("\b", "\\b")
				              .replace("\f", "\\f")
				              .replace("\n", "\\n")
				              .replace("\r", "\\r")
				              .replace("\t", "\\t");
	}

	@Override
	public String toString() {

		return new String("Note: " + getTitle() + " (" + getLastChangeDate() + ")");
	}
  
	public Note clone() {

		Note clone = new Note();

		clone.noteContent = noteContent;
		clone.xmlContent = xmlContent;
		clone.url = url;
		clone.fileName = fileName;
		clone.title = title;
		clone.lastChangeDate = lastChangeDate;
		clone.lastSyncRevision = lastSyncRevision;
		clone.dbId = dbId;
		clone.guid = guid;
		clone.tags = tags;

		return clone;

	}
	
}

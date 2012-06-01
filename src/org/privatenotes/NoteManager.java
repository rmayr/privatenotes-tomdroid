/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, 2010 Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2010       Rodja Trappe <mail@rodja.net> 
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License 
 * along with Tomdroid. If not, see <http://www.gnu.org/licenses/>.
 */
package org.privatenotes;

import java.util.UUID;

import org.privatenotes.sync.LocalStorage;
import org.privatenotes.ui.Tomdroid;
import org.privatenotes.util.NoteListCursorAdapter;
import org.privatenotes.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.ListAdapter;

public class NoteManager {
	public static final String[]	FULL_PROJECTION		= { Note.ID, Note.TITLE, Note.FILE,
			Note.NOTE_CONTENT, Note.MODIFIED_DATE, Note.GUID, Note.IS_SYNCED, Note.ENCRYPTED, Note.ENCRYPTED_ALGORITHM, Note.TAGS };
	public static final String[]	LIST_PROJECTION		= { Note.ID, Note.TITLE, Note.MODIFIED_DATE, Note.IS_SYNCED };
	public static final String[]	TITLE_PROJECTION	= { Note.TITLE };
	public static final String[]	GUID_PROJECTION		= { Note.ID, Note.GUID };
	public static final String[]	ID_PROJECTION		= { Note.ID };
	public static final String[]	EMPTY_PROJECTION	= {};

	// static properties
	private static final String		TAG					= "NoteManager";

	public static Note getNote(Context context, Uri uri) {
		Cursor cursor = context.getContentResolver().query(uri, FULL_PROJECTION, null, null, null);
		return getNote(cursor);
	}

	public static Note getNote(Context context, UUID guid) {
		String[] whereArgs = { guid.toString() };
		Cursor cursor = context.getContentResolver().query(Tomdroid.CONTENT_URI, FULL_PROJECTION,
				Note.GUID + "=?", whereArgs, null);

		return getNote(cursor);
	}

	/**
	 * Creates a Note which is described at the cursor position.
	 */
	public static Note getNote(Cursor cursor) {
		Note note = null;
		if (!(cursor == null || cursor.getCount() == 0)) {

			// create the note from the cursor
			cursor.moveToFirst();
			note = new Note(cursor);
		}
		cursor.close();
		return note;
	}

	public static void putNote(Activity activity, Note note) {

		// verify if the note is already in the content provider

		// TODO make the query prettier (use querybuilder)
		Uri notes = Tomdroid.CONTENT_URI;
		String[] whereArgs = new String[1];
		whereArgs[0] = note.getGuid().toString();

		// The note identifier is the guid
		ContentResolver cr = activity.getContentResolver();
		Cursor managedCursor = cr
				.query(notes, EMPTY_PROJECTION, Note.GUID + "= ?", whereArgs, null);
		activity.startManagingCursor(managedCursor);

		// Preparing the values to be either inserted or updated
		// depending on the result of the previous query
		ContentValues values = new ContentValues();
		values.put(Note.TITLE, note.getTitle());
		values.put(Note.FILE, note.getFileName());
		values.put(Note.GUID, note.getGuid().toString());
		values.put(Note.IS_SYNCED, note.isSynced() ? 1 : 0);
		// Notice that we store the date in UTC because sqlite doesn't handle RFC3339 timezone
		// information
		values.put(Note.MODIFIED_DATE, note.getLastChangeDate().format3339(false));
		values.put(Note.NOTE_CONTENT, note.getXmlContent());
		values.put(Note.TAGS, note.getTags());
		values.put(Note.ENCRYPTED, String.valueOf(note.isEncrypted()));
		if (managedCursor.getCount() == 0) {

			// This note is not in the database yet we need to insert it
			if (Tomdroid.LOGGING_ENABLED)
				Log.v(TAG, "A new note has been detected (not yet in db)");

			Uri uri = cr.insert(Tomdroid.CONTENT_URI, values);

			if (Tomdroid.LOGGING_ENABLED)
				Log.v(TAG, "Note inserted in content provider. ID: '" + uri + "', TITLE: '"
						+ note.getTitle() + "', GUID: '" + note.getGuid() + "',URI: " + uri);
		} else {

			// Overwrite the previous note if it exists
			cr.update(Tomdroid.CONTENT_URI, values, Note.GUID + " = ?", whereArgs);

			if (Tomdroid.LOGGING_ENABLED)
				Log.v(TAG, "Note updated in content provider. TITLE:" + note.getTitle() + " GUID:"
						+ note.getGuid());
		}
	}

	public static boolean deleteNote(Activity activity, int databaseId) {
		Uri uri = Uri.parse(Tomdroid.CONTENT_URI + "/" + databaseId);

		ContentResolver cr = activity.getContentResolver();
		int result = cr.delete(uri, null, null);

		if (result > 0)
			return true;
		else
			return false;
	}

	public static void deleteNote(Context context, UUID guid) {
		String[] whereArgs = { guid.toString() };
		context.getContentResolver().delete(Tomdroid.CONTENT_URI, Note.GUID + "=?", whereArgs);
	}
	
	public static Cursor getAllNotes(Activity activity, Boolean includeNotebookTemplates) {
		// get a cursor representing all notes from the NoteProvider
		Uri notes = Tomdroid.CONTENT_URI;
		String where = null;
		String orderBy;
		if (!includeNotebookTemplates)
		{
			where = Note.TAGS + " NOT LIKE '%" + "system:template" + "%'";
		}
		orderBy = Note.MODIFIED_DATE + " DESC";
		try {
		return activity.managedQuery(notes, LIST_PROJECTION, where, null, orderBy);
		} catch (Exception _e) {
			Log.e(TAG, "RESETTING DATABASE BECAUSE OF: ", _e);
			LocalStorage localStorage = new LocalStorage(activity);
			localStorage.resetDatabase();
			return null;
		}
	}

	public static ListAdapter getListAdapter(Activity activity)
	{

		// get a cursor representing all notes from the NoteProvider
		Cursor notesCursor = getAllNotes(activity, false);

		// set up an adapter binding the TITLE field of the cursor to the list
		// item
		String[] from = new String[] { Note.TITLE, Note.IS_SYNCED, Note.MODIFIED_DATE };
		int[] to = new int[] { R.id.note_title, R.id.note_date };
		return new NoteListCursorAdapter(activity, R.layout.main_list_item, notesCursor, from, to);
	}

	// gets the titles of the notes present in the db, used in ViewNote.buildLinkifyPattern()
	public static Cursor getTitles(Activity activity) {

		// get a cursor containing the notes titles
		return activity.managedQuery(Tomdroid.CONTENT_URI, TITLE_PROJECTION, null, null, null);
	}

	// gets the ids of the notes present in the db
	public static Cursor getGuids(Activity activity) {
		// get a cursor containing the notes guids
		return activity.managedQuery(Tomdroid.CONTENT_URI, GUID_PROJECTION, null, null, null);
	}

	public static int getNoteId(Activity activity, String title) {

		int id = 0;

		// get the notes ids
		String[] whereArgs = { title };
		Cursor cursor = activity.managedQuery(Tomdroid.CONTENT_URI, ID_PROJECTION, Note.TITLE
				+ "=?", whereArgs, null);

		// cursor must not be null and must return more than 0 entry
		if (!(cursor == null || cursor.getCount() == 0)) {

			cursor.moveToFirst();
			id = cursor.getInt(cursor.getColumnIndexOrThrow(Note.ID));
		} else {
			// TODO send an error to the user
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "Cursor returned null or 0 notes");
		}

		return id;
	}
}

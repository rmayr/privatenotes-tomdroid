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
package org.privatenotes.ui;

import org.privatenotes.Note;
import org.privatenotes.NoteManager;
import org.privatenotes.sync.LocalStorage;
import org.privatenotes.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * A GUI dialog which allows the user to create a new note (enter the title for a note
 * that should be created) 
 *
 */
public class NewNote extends DialogWithInputBox
{

	// Logging info
	private static final String TAG = "NewNote";
	
	private Activity last;
	private static NewNote instance = new NewNote();
	
	private NewNote() {
	}
	
	/**
	 * shows an input-dialog where you can enter the new notes title
	 * @param last
	 */
	public static void createNew(Activity last) {
		instance.last = last;
		instance.showDialog(last, last.getString(R.string.newNoteTitleRequest), "");
	}
	
	/**
	 * {@inheritDoc}
	 * this will create a new note with the entered text as a title
	 * if this was successful, the note will then be viewed
	 */
	@Override
	protected void onPositiveReaction(String text) {
		 saveFile(text);
		 // now show the newly created note:
		 // TODO: it might be better to query the db-id by the notes UUID
		int noteId = NoteManager.getNoteId(last, text);
		if (noteId > 0) {
			Uri intentUri = Uri.parse(Tomdroid.CONTENT_URI + "/" + noteId);
			Intent i = new Intent(Intent.ACTION_VIEW, intentUri, last, ViewNote.class);
			last.startActivity(i);
		} else
			Log.w(TAG, "could not find the newly created note!");
	}

	/**
	 * creates a new note with the given title
	 * @param title
	 */
	private void saveFile(String title)
	{
		Note n = new Note();
		n.setTitle(title);
		n.changeXmlContent(last.getString(R.string.newNoteDefaultContent));
		new LocalStorage(last).insertNote(n);
		
		Toast toast = Toast.makeText(last.getApplicationContext(), "Saved note locally", Toast.LENGTH_SHORT);
		toast.show();
	}

}

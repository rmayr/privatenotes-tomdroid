/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2010, Rodja Trappe <mail@rodja.net>
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
package org.tomdroid.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.LocalStorage;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.util.LinkifyPhone;
import org.tomdroid.util.NoteContentBuilder;
import org.tomdroid.util.XmlUtils;
import org.tomdroid.xml.NoteContentHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

// TODO this class is starting to smell
public class ViewNote extends Activity {

	// UI elements
	private TextView title;
	private EditText content;

	// Model objects
	private Note note;
	private SpannableStringBuilder noteContent;

	// Logging info
	private static final String TAG = "ViewNote";

	// UI feedback handler
	private SyncMessageHandler syncMessageHandler = new SyncMessageHandler(this);

	private LocalStorage localStorage;
	private ViewSwitcher viewSwitcher;
	private GestureDetector gestureDetector;

	// TODO extract methods in here
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.note_view);
		content = (EditText) findViewById(R.id.content);
		content.setBackgroundColor(0xffffffff);
		content.setTextColor(Color.DKGRAY);
		content.setTextSize(18.0f);
		title = (TextView) findViewById(R.id.title);
		title.setBackgroundColor(0xffdddddd);
		title.setTextColor(Color.DKGRAY);
		title.setTextSize(18.0f);

		final Intent intent = getIntent();
		handleUri(intent.getData());

		localStorage = new LocalStorage(this);
		ViewGroup container = (ViewGroup) findViewById(R.id.container);
		viewSwitcher = new ViewSwitcher(container);
		
		gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if (isInViewMode())
					switchToEditMode();
				else
					switchToViewMode();
				return true;
			}
		});
		
		viewNote();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (gestureDetector.onTouchEvent(event))
		 return true;
		return super.dispatchTouchEvent(event);
	}

	private void handleUri(Uri uri) {
		boolean successful = false;
		if (uri != null) {

			// We were triggered by an Intent URI
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "ViewNote started: Intent-filter triggered.");

			// TODO validate the good action?
			// intent.getAction()

			// TODO verify that getNote is doing the proper validation
			note = NoteManager.getNote(this, uri);

			if (note != null) {
				noteContent = note.getNoteContent(noteContentHandler);
				successful = true;
			}
		}

		if (!successful) {

			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "The Intent's data was null.");

			// TODO put error string in a translatable resource
			new AlertDialog.Builder(this)
					.setMessage(
							"The requested note could not be found. If you see this error "
									+ " and you are able to replicate it, please file a bug!")
					.setTitle("Error").setNeutralButton("Ok",
							new OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									finish();
								}
							}).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_notes, menu);
		return true;
	}
	
	/**
	 * this gets called every time opposed to "onCreateOptionsMenu"
	 */	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (isInEditMode()) {
			menu.findItem(R.id.menuEdit).setVisible(false);
			menu.findItem(R.id.menuView).setVisible(true);
			menu.findItem(R.id.styleEdit).setVisible(false);
		} else {
			menu.findItem(R.id.menuEdit).setVisible(true);
			menu.findItem(R.id.menuView).setVisible(false);
			menu.findItem(R.id.styleEdit).setVisible(true);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// rodjas items:
		case R.id.menuView:
			switchToViewMode();
			return true;
		case R.id.menuEdit:
			switchToEditMode();
			return true;
		case R.id.menuPrefs:
			startActivity(new Intent(this, PreferencesActivity.class));
			return true;
		// our items:
		case R.id.menuBigger:
			addTagToSelectedText("size:large");
			noteContent = note.getNoteContent(noteContentHandler);
			
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;
			// case R.id.menuSmaller:
			// addTagToSelectedText("<size:small>", "</size:small>");
			// noteContent = note.getNoteContent(noteContentHandler);
			// content.setText(noteContent, TextView.BufferType.SPANNABLE);
			// return true;
		case R.id.menuBold:
			addTagToSelectedText("bold");
			noteContent = note.getNoteContent(noteContentHandler);
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;

		case R.id.menuItalic:
			addTagToSelectedText("italic");
			noteContent = note.getNoteContent(noteContentHandler);
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;
		case R.id.menuHighligth:
			addTagToSelectedText("highlight");
			noteContent = note.getNoteContent(noteContentHandler);
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;
		case R.id.menuStrikeOut:
			addTagToSelectedText("strikethrough");
			noteContent = note.getNoteContent(noteContentHandler);
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;
		case R.id.menuFixedWidth:
			addTagToSelectedText("monospace");
			noteContent = note.getNoteContent(noteContentHandler);
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;
			// case R.id.menuURL:
			// addTagToSelectedText("<link:url>", "</link:url>");
			// noteContent = note.getNoteContent(noteContentHandler);
			// content.setText(noteContent, TextView.BufferType.SPANNABLE);
			// case R.id.menuBullets:
			// addTagToSelectedText("<list>", "</list>");
			// noteContent = note.getNoteContent(noteContentHandler);
			// content.setText(noteContent, TextView.BufferType.SPANNABLE);
			// return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void saveEditedContent() {
		TextView textView = (TextView) findViewById(R.id.editContent);
		String editedText = textView.getText().toString();
		String cleanedNoteText = NoteContentHandler.cleanNoteXmlOfTitle(note.getTitle(), note.getXmlContent());
		if (!editedText.equals(cleanedNoteText)) {
			note.changeXmlContent(NoteContentHandler.wrapContentWithTitleAndContentTag(note.getTitle(), editedText));
			note.isSynced(false);
			localStorage.insertNote(note);
			if (Tomdroid.LOGGING_ENABLED)
				Log.v(TAG, textView.getText().toString() + "\n----\n" + note.getXmlContent());
		}
	}

	private boolean isInEditMode() {
		return viewSwitcher.isBacksideVisible();
	}

	private boolean isInViewMode() {
		return viewSwitcher.isFrontsideVisible();
	}

	private void switchToEditMode() {
		if (isInEditMode()) {
			return;
		}
		viewSwitcher.swap();

		editNote();
	}
	
	private void viewNote() {
		//setTitle(note.getTitle());
		noteContent = note.getNoteContent(noteContentHandler);
		showNote();
	}
	
	private void editNote() {
		setTitle("Edit Mode");

		TextView textView = (TextView) findViewById(R.id.editContent);
		
		String editableText = NoteContentHandler.cleanNoteXmlOfTitle(note.getTitle(), note.getXmlContent());
		textView.setText(editableText);
	}

	private void switchToViewMode() {
		if (isInViewMode()) {
			return;
		}
		saveEditedContent();
		viewSwitcher.swap();
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);
		viewNote();
	}

	public void addTagToSelectedText(String tagName) {
		int start = content.getSelectionStart();
		int end = content.getSelectionEnd(); 

		String s = note.getXmlContent();

		if (end < start) {
			int temp = end;
			end = start;
			start = temp;
		}
		StringBuffer text = new StringBuffer(s);
		text = cleanNoteContentOfTitle(text);

		start = countAll(text, 0, start);
		end = countAll(text, 0, end);
		
		text = XmlUtils.safeInsertTag(text, tagName, start, end);
		
		note.changeXmlContent(NoteContentHandler.wrapContentWithTitleAndContentTag(note.getTitle(), text.toString()));
		note.isSynced(false);
		localStorage.insertNote(note);
	}
	
	/**
	 * counts all characters to a certain point. This is used when you only
	 * know how many visible characters there are until a certain position, but
	 * there are also xml-tags which aren't displayed
	 * @param text the text (possibly with xml tags)
	 * @param start where to start from with counting
	 * @param numberOfVisible how many visible chars we should count
	 * @return the total number of chars
	 */
	private int countAll(StringBuffer text, int start, int numberOfVisible) {
		int idx = start;
		int displayedLetters = 0;
		boolean tag = false;
		final int length = text.length();
		while (displayedLetters < numberOfVisible && idx < length) {
			if (tag) {
				if (text.charAt(idx) == '>') {
					tag = false;
				}
			} else {
				if (text.charAt(idx) == '<') {
					tag = true;
				} else {
					displayedLetters++;
				}
			}
			idx++;
		}
		return idx;
	}

	@Override
	public void onResume() {
		super.onResume();
		SyncManager.setActivity(this);
		SyncManager.setHandler(this.syncMessageHandler);
	}
	
	@Override
	public void onPause() {
		if (isInEditMode()) {
			saveEditedContent();
		}
		super.onPause();
	}
	
	public void setTitle(CharSequence title) {
		super.setTitle(title);
		// temporary setting title of actionbar until we have a better idea
		TextView titleView = (TextView) findViewById(R.id.title);
		titleView.setText(title);
	}
	
	/**
	 *  we need this for some sync methods
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode,
            android.content.Intent data) {
		syncMessageHandler.onActivityResult(this, requestCode, resultCode, data);
	}
	
	/**
	 * get rid of the title that is doubled in the note's content
	 * using quote to escape potential regexp chars in pattern
	 * @param noteContent
	 * @return
	 */
	private SpannableStringBuilder cleanNoteContentOfTitle(SpannableStringBuilder noteContent) {
		Pattern removeTitle = Pattern.compile("^\\s*"
				+ Pattern.quote(note.getTitle()) + "\\n\\n");
		Matcher m = removeTitle.matcher(noteContent);
		if (m.find()) {
			noteContent = noteContent.replace(0, m.end(), "");
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "stripped the title from note-content");
		}
		return noteContent;
	}
	
	/**
	 * does the same thing for a StringBuffer
	 * @param noteContent
	 * @return
	 */
	private StringBuffer cleanNoteContentOfTitle(StringBuffer noteContent) {
		Pattern removeTitle = Pattern.compile("^.*"
				+ Pattern.quote(note.getTitle()) + "\\n\\n");
		Matcher m = removeTitle.matcher(noteContent);
		if (m.find()) {
			noteContent = noteContent.replace(0, m.end(), "");
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "stripped the title from note-content");
		}
		// this is inefficient but works for now:
		String endTag = "</note-content>";
		if (noteContent.toString().endsWith(endTag)) {
			noteContent.setLength(noteContent.length() - endTag.length());
		}
		return noteContent;
	}

	private void showNote() {
		// setTitle(note.getTitle());

		noteContent = cleanNoteContentOfTitle(noteContent);

		// show the note (spannable makes the TextView able to output styled
		// text)
		// content.setText(noteContent, TextView.BufferType.SPANNABLE);
		title.setText((CharSequence) note.getTitle());

		// add links to stuff that is understood by Android except phone numbers
		// because it's too aggressive
		// TODO this is SLOWWWW!!!!
		Linkify.addLinks(noteContent, Linkify.EMAIL_ADDRESSES
				| Linkify.WEB_URLS | Linkify.MAP_ADDRESSES);

		// Custom phone number linkifier (fixes lp:512204)
		Linkify.addLinks(content, LinkifyPhone.PHONE_PATTERN, "tel:",
				LinkifyPhone.sPhoneNumberMatchFilter,
				Linkify.sPhoneNumberTransformFilter);

		// This will create a link every time a note title is found in the text.
		// The pattern contains a very dumb (title1)|(title2) escaped correctly
		// Then we transform the url from the note name to the note id to avoid
		// characters that mess up with the URI (ex: ?)
		Linkify.addLinks(content, buildNoteLinkifyPattern(),
				Tomdroid.CONTENT_URI + "/", null, noteTitleTransformFilter);
		// Linkify.addLinks(noteContent, Linkify.ALL);

		content.setText(noteContent, TextView.BufferType.SPANNABLE);
	}

	private Handler noteContentHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {


			 // parsed ok - show
			 if (msg.what == NoteContentBuilder.PARSE_OK)
			 {
				 showNote();
				
				 // parsed not ok - error
			 } else if (msg.what == NoteContentBuilder.PARSE_ERROR)
			 {
				 // TODO put this String in a translatable resource
				 new AlertDialog.Builder(ViewNote.this).setMessage(
					 "The requested note could not be parsed. If you see this error "
					 +
					" and you are able to replicate it, please file a bug!").setTitle(
					 "Error").setNeutralButton(
					 "Ok", new OnClickListener()
						 {
							 public void onClick(DialogInterface dialog, int which)
							 {
								 dialog.dismiss();
								 // go to edit mode, maybe the user can still fix it
								 switchToEditMode();
								 //finish();
							 }
						 }).show();
			 }
		}
	};

	/**
	 * Builds a regular expression pattern that will match any of the note title
	 * currently in the collection. Useful for the Linkify to create the links
	 * to the notes.
	 * 
	 * @return regexp pattern
	 */
	private Pattern buildNoteLinkifyPattern() {

		StringBuilder sb = new StringBuilder();
		Cursor cursor = NoteManager.getTitles(this);

		// cursor must not be null and must return more than 0 entry
		if (!(cursor == null || cursor.getCount() == 0)) {

			String title;

			cursor.moveToFirst();

			do {
				title = cursor.getString(cursor
						.getColumnIndexOrThrow(Note.TITLE));

				// Pattern.quote() here make sure that special characters in the
				// note's title are properly escaped
				sb.append("(" + Pattern.quote(title) + ")|");

			} while (cursor.moveToNext());

			// get rid of the last | that is not needed (I know, its ugly..
			// better
			// idea?)
			String pt = sb.substring(0, sb.length() - 1);

			// return a compiled match pattern
			return Pattern.compile(pt);

		} else {

			// TODO send an error to the user
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "Cursor returned null or 0 notes");
		}

		return null;
	}

	// custom transform filter that takes the note's title part of the URI and
	// translate it into the
	// note id
	// this was done to avoid problems with invalid characters in URI (ex: ? is
	// the query separator
	// but could be in a note title)
	private final TransformFilter noteTitleTransformFilter = new TransformFilter() {

		public String transformUrl(Matcher m, String str) {

			int id = NoteManager.getNoteId(ViewNote.this, str);

			// return something
			// like
			// content://org.tomdroid.notes/notes/3
			return Tomdroid.CONTENT_URI.toString() + "/" + id;
		}
	};
}

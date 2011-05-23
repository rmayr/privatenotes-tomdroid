/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
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
package org.tomdroid.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;

import org.tomdroid.util.LinkifyPhone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import at.fhooe.mcm.tomboyCrypt.CryptoScheme;
import at.fhooe.mcm.tomboyCrypt.CryptoSchemeV1;

// TODO this class is starting to smell
public class ViewNote extends Activity
{

	// UI elements
	private TextView title;
	private EditText content;

	// Model objects
	private Note note;
	private SpannableStringBuilder noteContent;

	// Logging info
	private static final String TAG = "ViewNote";

	// UI feedback handler
	private Handler syncMessageHandler = new SyncMessageHandler(this);

	private Uri mUri;

	// TODO extract methods in here
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
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
		mUri = intent.getData();

		if (mUri != null)
		{

			// We were triggered by an Intent URI
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "ViewNote started: Intent-filter triggered.");

			// TODO validate the good action?
			// intent.getAction()

			// TODO verify that getNote is doing the proper validation
			note = NoteManager.getNote(this, mUri);

			if (note != null)
			{

				// encryption
				if (!note.isEncrypted()) // if note is not encrypted
				{
					noteContent = note.getNoteContent(noteContentHandler);
				}
				// else
				// {
				// AES encryption
				// if (note.getEncryptedAlgorithm().trim().equalsIgnoreCase("AES"))
				// {
				// final AlertDialog.Builder alert = new AlertDialog.Builder(this);
				// final EditText input = new EditText(this);
				// input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
				// alert.setView(input);
				// alert.setPositiveButton("Ok", new
				// DialogInterface.OnClickListener()
				// {
				// public void onClick(DialogInterface dialog, int whichButton)
				// {
				// String value = input.getText().toString().trim();
				// encrypteAESNote(value);
				//
				// }
				// });
				//
				// alert.setNegativeButton("Cancel", new
				// DialogInterface.OnClickListener()
				// {
				// public void onClick(DialogInterface dialog, int whichButton)
				// {
				// dialog.cancel();
				// }
				// });
				// alert.show();
				//
				// }
				// }
				//
				// // Log.i(TAG, "THE NOTE IS: " +
				// // note.getXmlContent().toString());
				//
				// } else
				// {
				//
				// if (Tomdroid.LOGGING_ENABLED)
				// Log.d(TAG, "The note " + mUri + " doesn't exist");
				//
				// // TODO put error string in a translatable resource
				// new
				// AlertDialog.Builder(this).setMessage("The requested note could not be found. If you see this error "
				// + "and you are able to replicate it, please file a bug!")
				// .setTitle("Error").setNeutralButton("Ok", new OnClickListener()
				// {
				// public void onClick(DialogInterface dialog, int which)
				// {
				// dialog.dismiss();
				// finish();
				// }
				// }).show();
				// }

				// }

				else
				{

					if (Tomdroid.LOGGING_ENABLED)
						Log.d(TAG, "The Intent's data was null.");

					// TODO put error string in a translatable resource
					new AlertDialog.Builder(this).setMessage("The requested note could not be found. If you see this error " + " and you are able to replicate it, please file a bug!")
							.setTitle("Error").setNeutralButton("Ok", new OnClickListener()
							{
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
									finish();
								}
							}).show();
				}
			}
		}
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_notes, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menuBigger:
			addTagToSelectedText("<size:large>", "</size:large>");
			noteContent = note.getNoteContent(noteContentHandler);
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;
			// case R.id.menuSmaller:
			// addTagToSelectedText("<size:small>", "</size:small>");
			// noteContent = note.getNoteContent(noteContentHandler);
			// content.setText(noteContent, TextView.BufferType.SPANNABLE);
			// return true;
		case R.id.menuBold:
			addTagToSelectedText("<bold>", "</bold>");
			noteContent = note.getNoteContent(noteContentHandler);
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;

		case R.id.menuItalic:
			addTagToSelectedText("<italic>", "</italic>");
			noteContent = note.getNoteContent(noteContentHandler);
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;
		case R.id.menuHighligth:
			addTagToSelectedText("<highlight>", "</highlight>");
			noteContent = note.getNoteContent(noteContentHandler);
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;
		case R.id.menuStrikeOut:
			addTagToSelectedText("<strikethrough>", "</strikethrough>");
			noteContent = note.getNoteContent(noteContentHandler);
			content.setText(noteContent, TextView.BufferType.SPANNABLE);
			return true;
		case R.id.menuFixedWidth:
			addTagToSelectedText("<monospace>", "</monospace>");
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
		case R.id.menu_Save:

			saveFile();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void saveFile()
	{
		// if title is empty make a dialog
		if (title.getText().toString().compareTo("") == 0)
		{

			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("Error");
			alertDialog.setMessage("Please enter a title for the filename!");
			alertDialog.setButton("OK", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					return;
				}
			});
			alertDialog.show();
		} else
		{

			// String sFileName = title.getText() + ".note";
			String[] s = note.getFileName().split("/");

			// File root = new File(Tomdroid.NOTES_PATH);
			String sFileName = s[4].toString();
			File gpxfile = new File(Tomdroid.NOTES_PATH, sFileName);

			StringBuffer sb = new StringBuffer();
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			sb.append('\n');
			note.getXmlContent();
			sb
					.append("<note version=\"0.1\" xmlns:link=\"http://beatniksoftware.com/tomboy/link\" xmlns:size=\"http://beatniksoftware.com/tomboy/size\" xmlns=\"http://beatniksoftware.com/tomboy\">");
			sb.append('\n');

			sb.append("<title>" + title.getText() + "</title>");
			sb.append('\n');
			// <note-content version=\"0.1\">" + content.getText() +
			// "</note-content>
			sb.append("<text xml:space=\"preserve\">" + note.getXmlContent() + "</text>");

			sb.append('\n');
			sb.append("  <last-change-date>2011-01-27T11:04:26.2892499+01:00</last-change-date>");
			sb.append('\n');
			sb.append("  <last-metadata-change-date>2011-01-27T11:04:26.2892499+01:00</last-metadata-change-date>");
			sb.append('\n');
			sb.append("  <create-date>2010-11-18T15:31:15.0012854+01:00</create-date>");
			sb.append('\n');
			sb.append("  <cursor-position>326</cursor-position>");
			sb.append('\n');
			sb.append("  <width>477</width>");
			sb.append('\n');
			sb.append("  <height>408</height>");
			sb.append('\n');
			sb.append("  <x>1897</x>");
			sb.append('\n');
			sb.append("  <y>298</y>");
			sb.append('\n');
			sb.append("  <open-on-startup>False</open-on-startup>");
			sb.append('\n');
			sb.append("");
			sb.append("</note>");

			/**
			 * Encrypt file file, Text, password
			 */
			CryptoScheme schemeToTest = new CryptoSchemeV1();
			schemeToTest.writeFile(gpxfile, sb.toString().getBytes(), Tomdroid.getInstance().getPassword().getBytes());

			Toast toast = Toast.makeText(getApplicationContext(), "Saved file in " + Tomdroid.NOTES_PATH, Toast.LENGTH_SHORT);
			toast.show();

		}
	}

	/**
	 * 
	 * get the selected Text
	 * @param startTag
	 * @param endTag
	 */
	public void addTagToSelectedText(String startTag, String endTag)
	{
		int start = content.getSelectionStart();
		int end = content.getSelectionEnd();
		// length of the tags
		int countStartTagLetters = startTag.length();
		int countEndTagLetters = endTag.length();

		CryptoScheme decrypt = new CryptoSchemeV1();
		note.getFileName();
		File f = new File(note.getFileName().toString());
		byte[] decrypted = decrypt.decryptFile(f, Tomdroid.getInstance().getPassword().getBytes());
		// String noteText = new String(decrypted);

		// String s = content.getText().toString();
		String s = note.getXmlContent();

		if (end < start)
		{
			int temp = end;
			end = start;
			start = temp;
		}
		// StringBuffer text = new StringBuffer(note.getXmlContent());
		StringBuffer text = new StringBuffer(s);

		int displayedLetters = 0;
		int tagLetters = 0;
		boolean tag = false;

		int first = 0;
		if (start == 0)
		{
			while (text.charAt(first) != '>')
			{
				first++;
			}
			first++;
		} else
		{
			while (displayedLetters < start)
			{
				if (tag)
				{
					if (text.charAt(displayedLetters + first) == '>')
					{
						tag = false;
					}
					first++;
				} else
				{
					if (text.charAt(displayedLetters + first) == '<')
					{
						tag = true;
						first++;
					} else
					{
						displayedLetters++;
					}
				}
			}
		}

		if (text.substring(start + first, start + first + countStartTagLetters).contains(startTag))
		{

			text.delete((end - start) + start + first + countStartTagLetters, (end - start) + start + countStartTagLetters + first + countEndTagLetters);
			text.delete(start + first, start + first + countStartTagLetters);
		} else
		{
			text.insert(end + first, endTag);
			text.insert(start + first, startTag);
		}
		// content.setText(text.toString());
		note.setXmlContent(text.toString());

	}

	@Override
	public void onResume()
	{
		super.onResume();
		SyncManager.setActivity(this);
		SyncManager.setHandler(this.syncMessageHandler);
	}

	@Override
	protected void onStop()
	{
		// String text = content.getText().toString();
		// int length = text.length();
		//
		//
		// ContentValues values = new ContentValues();
		//
		// // This stuff is only done when working with a full-fledged note.
		//      
		// // Bump the modification time to now.
		// values.put(Note.MODIFIED_DATE, "2010-10-09T20:50:12.000Z");
		//
		// // If we are creating a new note, then we want to also create
		// // an initial title for it.
		//             
		// String title = text.substring(0, Math.min(30, length));
		// if (length > 30) {
		// int lastSpace = title.lastIndexOf(' ');
		// if (lastSpace > 0) {
		// title = title.substring(0, lastSpace);
		// }
		// }
		// values.put(Note.TITLE, title);
		//              
		//          
		//
		// // Write our text back into the provider.
		// values.put(Note.NOTE_CONTENT, text);
		//
		// // Commit all of our changes to persistent storage. When the update
		// completes
		// // the content provider will notify the cursor of the change, which
		// will
		// // cause the UI to be updated.
		// getContentResolver().update(mUri, values, null, null);
		super.onStop();
	}

	private void encrypteAESNote(String password)
	{

		String msg = null;
		try
		{
			FileInputStream fin = null;
			fin = new FileInputStream(note.getFileName());
			int data2 = fin.read();
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			while (data2 >= 0)
			{
				bout.write(data2);
				data2 = fin.read();
			}
			byte[] data = bout.toByteArray();

			String help = new String(data);

			int startIndex = help.indexOf('>', help.indexOf("<note-content")) + 1;
			int encLength = help.substring(startIndex).lastIndexOf("</note-content>") + (data.length - help.length());
			byte[] encData = new byte[encLength];
			System.arraycopy(data, startIndex, encData, 0, encData.length);

			noteContent = note.getNoteContent(noteContentHandler);
		} 

		catch (IOException e)
		{
			msg = "io exception: " + e.getMessage();
		} catch (Exception e)
		{
			msg = "exception: " + e.getMessage();
		}

		if (msg != null)
		{
			new AlertDialog.Builder(this).setMessage(msg).setTitle("Error").setNeutralButton("Ok", new OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
					finish();
				}
			}).show();
		}
	}

	private void showNote()
	{
		// setTitle(note.getTitle());

		// get rid of the title that is doubled in the note's content
		// using quote to escape potential regexp chars in pattern
		Pattern removeTitle = Pattern.compile("^\\s*" + Pattern.quote(note.getTitle()) + "\\n\\n");
		Matcher m = removeTitle.matcher(noteContent);
		if (m.find())
		{
			noteContent = noteContent.replace(0, m.end(), "");
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "stripped the title from note-content");
		}

		// show the note (spannable makes the TextView able to output styled
		// text)
		// content.setText(noteContent, TextView.BufferType.SPANNABLE);
		title.setText((CharSequence) note.getTitle());

		// add links to stuff that is understood by Android except phone numbers
		// because it's too aggressive
		// TODO this is SLOWWWW!!!!
		Linkify.addLinks(noteContent, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS | Linkify.MAP_ADDRESSES);

		// Custom phone number linkifier (fixes lp:512204)
		Linkify.addLinks(content, LinkifyPhone.PHONE_PATTERN, "tel:", LinkifyPhone.sPhoneNumberMatchFilter, Linkify.sPhoneNumberTransformFilter);

		// This will create a link every time a note title is found in the text.
		// The pattern contains a very dumb (title1)|(title2) escaped correctly
		// Then we transform the url from the note name to the note id to avoid
		// characters that mess up with the URI (ex: ?)
		Linkify.addLinks(content, buildNoteLinkifyPattern(), Tomdroid.CONTENT_URI + "/", null, noteTitleTransformFilter);
		// Linkify.addLinks(noteContent, Linkify.ALL);

		content.setText(noteContent, TextView.BufferType.SPANNABLE);
	}

	public void setTitle(CharSequence title)
	{
		super.setTitle(title);
		// temporary setting title of actionbar until we have a better idea
		TextView titleView = (TextView) findViewById(R.id.title);
		titleView.setText(title);
	}

	private Handler noteContentHandler = new Handler()
	{

		@Override
		public void handleMessage(Message msg)
		{
			showNote();
			return;

			// // parsed ok - show
			// if (msg.what == NoteContentBuilder.PARSE_OK)
			// {
			// showNote();
			//
			// // parsed not ok - error
			// } else if (msg.what == NoteContentBuilder.PARSE_ERROR)
			// {
			//
			// // TODO put this String in a translatable resource
			// new AlertDialog.Builder(ViewNote.this).setMessage(
			// "The requested note could not be parsed. If you see this error "
			// +
			// " and you are able to replicate it, please file a bug!").setTitle("Error").setNeutralButton(
			// "Ok", new OnClickListener()
			// {
			// public void onClick(DialogInterface dialog, int which)
			// {
			// dialog.dismiss();
			// finish();
			// }
			// }).show();
			// }
		}
	};

	/**
	 * Builds a regular expression pattern that will match any of the note title
	 * currently in the collection. Useful for the Linkify to create the links to
	 * the notes.
	 * 
	 * @return regexp pattern
	 */
	private Pattern buildNoteLinkifyPattern()
	{

		StringBuilder sb = new StringBuilder();
		Cursor cursor = NoteManager.getTitles(this);

		// cursor must not be null and must return more than 0 entry
		if (!(cursor == null || cursor.getCount() == 0))
		{

			String title;

			cursor.moveToFirst();

			do
			{
				title = cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE));

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

		} else
		{

			// TODO send an error to the user
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "Cursor returned null or 0 notes");
		}

		return null;
	}

	// custom transform filter that takes the note's title part of the URI and
	// translate it into the note id
	// this was done to avoid problems with invalid characters in URI (ex: ? is
	// the query separator but could be in a note title)
	private TransformFilter noteTitleTransformFilter = new TransformFilter()
	{

		public String transformUrl(Matcher m, String str)
		{

			int id = NoteManager.getNoteId(ViewNote.this, str);

			// return something like content://org.tomdroid.notes/notes/3
			return Tomdroid.CONTENT_URI.toString() + "/" + id;
		}
	};
}

package org.tomdroid.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings.TextSize;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;
import at.fhooe.mcm.tomboyCrypt.CryptoScheme;
import at.fhooe.mcm.tomboyCrypt.CryptoSchemeV1;

public class NewNote extends Activity
{

	// UI elements
	private EditText title;
	private EditText content;

	// Model objects
	private Note note;
	private SpannableStringBuilder noteContent;

	// Logging info
	private static final String TAG = "NewNote";

	// UI feedback handler
	private Handler syncMessageHandler = new SyncMessageHandler(this);

	private Uri mUri;

	// TODO extract methods in here
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.new_note);
		content = (EditText) findViewById(R.id.newNoteText);

		title = (EditText) findViewById(R.id.newNoteTitle);

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
			if (note != null)
			{
				noteContent = note.getNoteContent(noteContentHandler);
				content.setText(noteContent, TextView.BufferType.SPANNABLE);
			}
			return true;
			// case R.id.menuSmaller:
			// addTagToSelectedText("<size:small>", "</size:small>");
			// if (note != null)
			// {
			// noteContent = note.getNoteContent(noteContentHandler);
			// content.setText(noteContent, TextView.BufferType.SPANNABLE);
			// }
			// return true;
		case R.id.menuBold:
			addTagToSelectedText("<bold>", "</bold>");
			if (note != null)
			{
				noteContent = note.getNoteContent(noteContentHandler);
				content.setText(noteContent, TextView.BufferType.SPANNABLE);
			}

			return true;

		case R.id.menuItalic:
			addTagToSelectedText("<italic>", "</italic>");
			if (note != null)
			{
				noteContent = note.getNoteContent(noteContentHandler);
				content.setText(noteContent, TextView.BufferType.SPANNABLE);
			}
			return true;
		case R.id.menuHighligth:
			addTagToSelectedText("<highlight>", "</highlight>");
			if (note != null)
			{
				noteContent = note.getNoteContent(noteContentHandler);
				content.setText(noteContent, TextView.BufferType.SPANNABLE);
			}
			return true;
		case R.id.menuStrikeOut:
			addTagToSelectedText("<strikethrough>", "</strikethrough>");
			if (note != null)
			{
				noteContent = note.getNoteContent(noteContentHandler);
				content.setText(noteContent, TextView.BufferType.SPANNABLE);
			}
			return true;
		case R.id.menuFixedWidth:
			addTagToSelectedText("<monospace>", "</monospace>");
			if (note != null)
			{
				noteContent = note.getNoteContent(noteContentHandler);
				content.setText(noteContent, TextView.BufferType.SPANNABLE);
			}
			return true;

			// case R.id.menuURL:
			// addTagToSelectedText("<link:url>", "</link:url>");
			// if (note != null)
			// {
			// noteContent = note.getNoteContent(noteContentHandler);
			// content.setText(noteContent, TextView.BufferType.SPANNABLE);
			// }
			// return true;
			// case R.id.menuBullets:
			// addTagToSelectedText("<list>", "</list>");
			// if (note != null)
			// {
			// noteContent = note.getNoteContent(noteContentHandler);
			// content.setText(noteContent, TextView.BufferType.SPANNABLE);
			// }
			// return true;

		case R.id.menu_Save:
			saveFile();
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Used to create a temporary file to save the xml content
	 */
	private void saveTempFile()
	{
		try
		{
			String sFileName = "Test1.note";

			File root = Environment.getExternalStorageDirectory();
			File gpxfile = new File(root + "/tomdroid/", sFileName);
			FileWriter writer = new FileWriter(gpxfile);

			writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			writer.append('\n');
			writer.append("<note version=\"0.1\" xmlns:link=\"http://beatniksoftware.com/tomboy/link\" xmlns:size=\"http://beatniksoftware.com/tomboy/size\" xmlns=\"http://beatniksoftware.com/tomboy\">");
			writer.append('\n');

			writer.append("<title>" + title.getText() + "</title>");
			writer.append('\n');
			writer.append("<text xml:space=\"preserve\"><note-content version=\"0.1\">" + content.getText() + "</note-content></text>");

			writer.append('\n');
			writer.append("  <last-change-date>2011-01-27T11:04:26.2892499+01:00</last-change-date>");
			writer.append('\n');
			writer.append("  <last-metadata-change-date>2011-01-27T11:04:26.2892499+01:00</last-metadata-change-date>");
			writer.append('\n');
			writer.append("  <create-date>2010-11-18T15:31:15.0012854+01:00</create-date>");
			writer.append('\n');
			writer.append("  <cursor-position>326</cursor-position>");
			writer.append('\n');
			writer.append("  <width>477</width>");
			writer.append('\n');
			writer.append("  <height>408</height>");
			writer.append('\n');
			writer.append("  <x>1897</x>");
			writer.append('\n');
			writer.append("  <y>298</y>");
			writer.append('\n');
			writer.append("  <open-on-startup>False</open-on-startup>");
			writer.append('\n');
			writer.append("");
			writer.append("</note>");

			writer.flush();
			writer.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

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
			String sFileName = title.getText() + ".note";

			File file = new File(Tomdroid.NOTES_PATH, sFileName);

			StringBuffer sb = new StringBuffer();
			sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			sb.append('\n');
			sb.append("<note version=\"0.1\" xmlns:link=\"http://beatniksoftware.com/tomboy/link\" xmlns:size=\"http://beatniksoftware.com/tomboy/size\" xmlns=\"http://beatniksoftware.com/tomboy\">");
			sb.append('\n');

			sb.append("<title>" + title.getText() + "</title>");
			sb.append('\n');
			sb.append("<text xml:space=\"preserve\"><note-content version=\"0.1\">" + content.getText() + "</note-content></text>");

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
			schemeToTest.writeFile(file, sb.toString().getBytes(), Tomdroid.getInstance().getPassword().getBytes());

			Toast toast = Toast.makeText(getApplicationContext(), "Saved file in " + Tomdroid.NOTES_PATH, Toast.LENGTH_SHORT);
			toast.show();

		}
	}

	public void addTagToSelectedText(String startTag, String endTag)
	{

		int start = content.getSelectionStart();
		int end = content.getSelectionEnd();
		// length of the tags
		int countStartTagLetters = startTag.length();
		int countEndTagLetters = endTag.length();

		String s = content.getText().toString();
		// String s = note.getXmlContent();

		if (end < start)
		{
			int temp = end;
			end = start;
			start = temp;
		}
		// StringBuffer text = new StringBuffer(note.getXmlContent());
		StringBuffer text = new StringBuffer(s);

		if (start + (end - start) + countEndTagLetters <= text.length() && start - countStartTagLetters >= 0)
		{
			if (text.substring(start - countStartTagLetters, start).contains(startTag))
			{

				text.delete((end - start) + start, (end - start) + start + countEndTagLetters);
				text.delete(start - countStartTagLetters, start);
			} else
			{
				text.insert(end, endTag);
				text.insert(start, startTag);
			}
		} else
		{
			text.insert(end, endTag);
			text.insert(start, startTag);
		}
		content.setText(text.toString());

		// note.setXmlContent(text.toString());

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
		super.onStop();
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

		title.setText((CharSequence) note.getTitle());

		// add links to stuff that is understood by Android except phone numbers
		// because it's too aggressive
		// TODO this is SLOWWWW!!!!
		Linkify.addLinks(content, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS | Linkify.MAP_ADDRESSES);

		// Custom phone number linkifier (fixes lp:512204)
		Linkify.addLinks(content, LinkifyPhone.PHONE_PATTERN, "tel:", LinkifyPhone.sPhoneNumberMatchFilter, Linkify.sPhoneNumberTransformFilter);

		// This will create a link every time a note title is found in the text.
		// The pattern contains a very dumb (title1)|(title2) escaped correctly
		// Then we transform the url from the note name to the note id to avoid
		// characters that mess up with the URI (ex: ?)
		Linkify.addLinks(content, buildNoteLinkifyPattern(), Tomdroid.CONTENT_URI + "/", null, noteTitleTransformFilter);

		content.setText(noteContent, TextView.BufferType.SPANNABLE);
	}

	private Handler noteContentHandler = new Handler()
	{

		@Override
		public void handleMessage(Message msg)
		{
			showNote();
			return;

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

			int id = NoteManager.getNoteId(NewNote.this, str);

			// return something like content://org.tomdroid.notes/notes/3
			return Tomdroid.CONTENT_URI.toString() + "/" + id;
		}
	};

}

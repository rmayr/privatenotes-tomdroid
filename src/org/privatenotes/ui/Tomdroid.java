/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
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
package org.privatenotes.ui;

import org.privatenotes.Note;
import org.privatenotes.NoteManager;
import org.privatenotes.sync.LocalStorage;
import org.privatenotes.sync.ServiceAuth;
import org.privatenotes.sync.SyncManager;
import org.privatenotes.sync.SyncMethod;
import org.privatenotes.util.FirstNote;
import org.privatenotes.util.Preferences;
import org.privatenotes.util.SecurityUtil;
import org.privatenotes.R;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Tomdroid extends ListActivity
{

	// Global definition for Tomdroid
	public static final String AUTHORITY = "org.privatenotes.notes";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.tomdroid.note";
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.tomdroid.note";
	public static final String PROJECT_HOMEPAGE = "http://privatenotes.dyndns-server.com/";

	// config parameters
	// TODO hardcoded for now
	public static final String NOTES_PATH = Environment.getExternalStorageDirectory() + "/tomdroid/";
	// Logging should be disabled for release builds
	public static final boolean	LOGGING_ENABLED		= true;
	// Set this to false for release builds, the reason should be obvious
	public static final boolean CLEAR_PREFERENCES = false;

	// Logging info
	private static final String TAG = "PrivateNotes";

	// UI to data model glue
	private TextView listEmptyView;
	private ListAdapter adapter;

	private static Tomdroid m_insTomdroid;

	// UI feedback handler
	private final SyncMessageHandler syncMessageHandler = new SyncMessageHandler(this);

	/** Called when the activity is created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		Preferences.init(this, CLEAR_PREFERENCES);
		m_insTomdroid = this;
		// did we already show the warning and got destroyed by android's activity
		// killer?
		if (Preferences.getBoolean(Preferences.Key.FIRST_RUN))
		{
			Log.i(TAG, "PrivateNotes is first run.");

			// add a first explanatory note
			NoteManager.putNote(this, FirstNote.createFirstNote());

			// Warn that this is a "will eat your babies" release
			new AlertDialog.Builder(this).setMessage(getString(R.string.strWelcome)).setTitle("Welcome").setNeutralButton("Ok", new OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					Preferences.putBoolean(Preferences.Key.FIRST_RUN, false);
					dialog.dismiss();

				}
			}).setIcon(R.drawable.icon).show();
		}
		// else the notes are also listed if the app is started more then one time
		else
		{
			adapter = NoteManager.getListAdapter(Tomdroid.this);
			setListAdapter(adapter);

			// set the view shown when the list is empty
			// TODO default empty-list text is butt-ugly!
			listEmptyView = (TextView) findViewById(R.id.list_empty);
			getListView().setEmptyView(listEmptyView);
		}
		
		Boolean savePw = Preferences.getBoolean(Preferences.Key.SAVE_PASSWORD);
		if (Preferences.getBoolean(Preferences.Key.FIRST_RUN)
				|| !savePw
				|| (savePw && SecurityUtil.getInstance().getPassword().equals(""))) {
			showPasswordEntryDialog(false);
		}
		if (SecurityUtil.getInstance().needsGpgPassword()) {
			showPasswordEntryDialog(true);
		}
		
		// handle share links that have been clicked/activated from somewhere
		Intent i = getIntent();		
		if (i != null && i.getData() != null) {
			String param = i.getData().toString();
			if (param.startsWith(ImportShare.SHARELINKPREFIX)) {
				param = param.substring(ImportShare.SHARELINKPREFIX.length());
				ImportShare.createNew(this, param);
			}
		}
	}


	public static Tomdroid getInstance()
	{
		return m_insTomdroid;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		// Create the menu based on what is defined in res/menu/main.xml
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menuAbout:
			showAboutDialog();
			return true;

		case R.id.menuPrefs:
			startActivity(new Intent(this, PreferencesActivity.class));
			return true;

		case R.id.menuNewNote:
			NewNote.createNew(this);
			return true;
		case R.id.menuImportShare:
			ImportShare.createNew(this);
			return true;
		case R.id.menuRevert:
			LocalStorage localStorage = new LocalStorage(this);
			localStorage.resetDatabase();
			// we put -2 because we get -1 if there is no sync data on the server
			// but then we still want to sync, and for that the numbers must not match
			Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, -2);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume();
		Intent intent = this.getIntent();

		SyncMethod currentService = SyncManager.getInstance().getCurrentSyncMethod();

		if (currentService.needsAuth() && intent != null)
		{
			Uri uri = intent.getData();

			if (uri != null && uri.getScheme().equals("tomdroid"))
			{
				Log.i(TAG, "Got url : " + uri.toString());
				SyncMethod currentSyncMethod = SyncManager.getInstance().getCurrentSyncMethod();

				if (currentSyncMethod.needsAuth()) {
					final ProgressDialog dialog = ProgressDialog.show(this, "",
							"Completing authentication. Please wait...", true, false);
					
					Handler handler = new Handler() {
						
						@Override
						public void handleMessage(Message msg) {
							dialog.dismiss();
						}
						
					};

					((ServiceAuth) currentService).remoteAuthComplete(uri, handler);
				}
			}

		}
		SyncManager.setActivity(this);
		SyncManager.setHandler(this.syncMessageHandler);
	}
	
	@Override
	protected void onDestroy() {
		if (isFinishing()) {
			if (Boolean.FALSE.equals(Preferences.getBoolean(Preferences.Key.SAVE_PASSWORD))) {
				Log.i(TAG, "finally removing pw from temporary memory");
				SecurityUtil.getInstance().setPassword("".getBytes());
			}
		}
		super.onDestroy();
	}

	
	@Override
	public void onBackPressed() {
		finish();
	}
	
	public static void showPasswordEntryDialog(final boolean isGpgPassword) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(Tomdroid.m_insTomdroid);
		final EditText input = new EditText(Tomdroid.m_insTomdroid);
		input.setTransformationMethod(new PasswordTransformationMethod());
		String title = isGpgPassword?"Please enter your Gpg Key passphrase!":"Please enter a passwort for en- and decryption!";
		alert.setTitle(title);
		alert.setIcon(R.drawable.ic_menu_login);
		alert.setView(input);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					// save password
					if (isGpgPassword) {
						SecurityUtil.getInstance().setGpgPassword(input.getText().toString().getBytes());
					} else {						
						SecurityUtil.getInstance().setPassword(input.getText().toString().getBytes());
					}
				}
			});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					//TODO dialog reopen on a new start
				}
			});
		
		alert.show();
	}

	private void showAboutDialog()
	{

		// grab version info
		String ver;
		try
		{
			ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e)
		{
			e.printStackTrace();
			ver = "Not found!";
		}

		// format the string
		String aboutDialogFormat = getString(R.string.strAbout);
		String aboutDialogStr = String.format(aboutDialogFormat, getString(R.string.app_desc), 
				getString(R.string.author), // Author name
				ver // Version
				);

		// build and show the dialog
		new AlertDialog.Builder(this).setMessage(aboutDialogStr).setTitle("About PrivateNotes").setIcon(R.drawable.icon).setNegativeButton("Project page", new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Tomdroid.PROJECT_HOMEPAGE)));
				dialog.dismiss();
			} 
		}).setPositiveButton("Ok", new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		}).show();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		Cursor item = (Cursor) adapter.getItem(position);
		int noteId = item.getInt(item.getColumnIndexOrThrow(Note.ID));

		Uri intentUri = Uri.parse(Tomdroid.CONTENT_URI + "/" + noteId);
		Intent i = new Intent(Intent.ACTION_VIEW, intentUri, this, ViewNote.class);
		startActivity(i);
	}
	
	/**
	 *  we need this for some sync methods
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode,
            android.content.Intent data) {
		syncMessageHandler.onActivityResult(this, requestCode, resultCode, data);
	}

}

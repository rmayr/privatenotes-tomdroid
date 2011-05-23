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
package org.tomdroid.ui;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;
import org.tomdroid.util.FirstNote;
import org.tomdroid.util.Preferences;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
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
	public static final String AUTHORITY = "org.tomdroid.notes";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.tomdroid.note";
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.tomdroid.note";
	public static final String PROJECT_HOMEPAGE = "http://www.launchpad.net/tomdroid/";

	// config parameters
	// TODO hardcoded for now
	public static final String NOTES_PATH = Environment.getExternalStorageDirectory() + "/tomdroid/";
	// Logging should be disabled for release builds
	public static final boolean LOGGING_ENABLED = false;
	// Set this to false for release builds, the reason should be obvious
	public static final boolean CLEAR_PREFERENCES = false;

	// Password storage fields
	private final String PASSWORD_STORAGE = "password_storage";
	private final String PASSWORD_FIELD = "password_field";

	// Logging info
	private static final String TAG = "Tomdroid";

	// UI to data model glue
	private TextView listEmptyView;
	private ListAdapter adapter;

	private static Tomdroid m_insTomdroid;

	// UI feedback handler
	private Handler syncMessageHandler = new SyncMessageHandler(this);

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
			Log.i(TAG, "Tomdroid is first run.");

			// add a first explanatory note
			NoteManager.putNote(this, FirstNote.createFirstNote());

			
			new AlertDialog.Builder(this).setMessage(getString(R.string.strWelcome)).setTitle("Welcome").setNeutralButton("Ok", new OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					Preferences.putBoolean(Preferences.Key.FIRST_RUN, false);
					dialog.dismiss();

					final AlertDialog.Builder alert = new AlertDialog.Builder(Tomdroid.this);
					final EditText input = new EditText(Tomdroid.this);
					input.setTransformationMethod(new PasswordTransformationMethod());
					alert.setTitle("Please enter a passwort for en- and decryption!");
					alert.setIcon(R.drawable.ic_menu_login);
					alert.setView(input);
					alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int whichButton)
						{
							// save password
							SharedPreferences profileStored = getSharedPreferences(PASSWORD_STORAGE, 0);
							SharedPreferences.Editor editor = profileStored.edit();

							try
							{
								editor.putString(PASSWORD_FIELD, input.getText().toString());
								editor.commit();
							} catch (Exception _ex)
							{
								_ex.printStackTrace();
							}

							// adapter that binds the ListView UI to the notes in the
							// note manager
							adapter = NoteManager.getListAdapter(Tomdroid.this);
							setListAdapter(adapter);

							// set the view shown when the list is empty
							// TODO default empty-list text is butt-ugly!
							listEmptyView = (TextView) findViewById(R.id.list_empty);
							getListView().setEmptyView(listEmptyView);
						}
					});

					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int whichButton)
						{
							//TODO dialog reopen on a new start
							finish();
						}
					});
					alert.show();

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

	}

	/**
	 * load password from storage
	 * 
	 * @return
	 */
	public String getPassword()
	{
		SharedPreferences profileStored = getSharedPreferences(PASSWORD_STORAGE, 0);

		try
		{
			return profileStored.getString(PASSWORD_FIELD, "");

		} catch (Exception _ex)
		{
			return "";
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
			// activity
			startActivity(new Intent(this, NewNote.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void onResume()
	{
		super.onResume();
		Intent intent = this.getIntent();

		SyncService currentService = SyncManager.getInstance().getCurrentService();

		if (currentService.needsAuth() && intent != null)
		{
			Uri uri = intent.getData();

			if (uri != null && uri.getScheme().equals("tomdroid"))
			{
				Log.i(TAG, "Got url : " + uri.toString());

				final ProgressDialog dialog = ProgressDialog.show(this, "", "Completing authentication. Please wait...", true, false);

				Handler handler = new Handler()
				{

					@Override
					public void handleMessage(Message msg)
					{
						dialog.dismiss();
					}

				};

				((ServiceAuth) currentService).remoteAuthComplete(uri, handler);
			}
		}

		SyncManager.setActivity(this);
		SyncManager.setHandler(this.syncMessageHandler);
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
		String aboutDialogStr = String.format(aboutDialogFormat, getString(R.string.app_desc), // App
				// description
				getString(R.string.author), // Author name
				ver // Version
				);

		// build and show the dialog
		new AlertDialog.Builder(this).setMessage(aboutDialogStr).setTitle("About Tomdroid").setIcon(R.drawable.icon).setNegativeButton("Project page", new OnClickListener()
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

}

/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2010, Rodja Trappe <mail@rodja.net>
 * Copyright 2010, Olivier Bilodeau <olivier@bottomlesspit.org>
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

import java.util.ArrayList;
import java.util.List;

import org.privatenotes.NoteManager;
import org.privatenotes.sync.ServiceAuth;
import org.privatenotes.sync.SyncManager;
import org.privatenotes.sync.SyncMethod;
import org.privatenotes.util.FirstNote;
import org.privatenotes.util.Preferences;
import org.privatenotes.util.SecurityUtil;
import org.privatenotes.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity
{

	private static final String TAG = "PreferencesActivity";

	// TODO: put the various preferences in fields and figure out what to do on
	// activity suspend/resume
	private EditTextPreference syncServerUriPreference = null;
	private ListPreference syncMethodPreference = null;	
	private CheckBoxPreference persistPasswordPreference = null;
	private ListPreference syncSharesPreference = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// Fill the Preferences fields
		syncServerUriPreference = (EditTextPreference)findPreference(Preferences.Key.SYNC_SERVER_URI.getName());
		syncServerUriPreference.getEditText().setSingleLine(); // only one line allowed
		syncMethodPreference = (ListPreference)findPreference(Preferences.Key.SYNC_METHOD.getName());
		persistPasswordPreference = (CheckBoxPreference)findPreference(Preferences.Key.SAVE_PASSWORD.getName());
		syncSharesPreference = (ListPreference)findPreference("sharesList");
		
		// Set the default values if nothing exists
		this.setDefaults();
		
		fillSharesList();

		fillSyncServicesList();

		// Enable or disable the server field depending on the selected sync
		// service
		updatePreferencesTo(syncMethodPreference.getValue());
		
		syncMethodPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String selectedSyncServiceKey = (String)newValue;
				
				// did the selection change?
				if (!syncMethodPreference.getValue().contentEquals(selectedSyncServiceKey)) {
					Log.d(TAG, "preference change triggered");

					syncServiceChanged(selectedSyncServiceKey);
				}
				return true;
			}
		});

		// Re-authenticate if the sync server changes
		syncServerUriPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference, Object serverUri)
			{
				if (serverUri == null)
				{
					Toast.makeText(PreferencesActivity.this, getString(R.string.prefServerEmpty), Toast.LENGTH_SHORT).show();
					return false;
				}
				
				// sometimes we end up with linebreaks in there, remove them
				String uri = (String)serverUri;
				uri = uri.replace("\r", "").replace("\n", "");

				// update the value before doing anything
				Preferences.putString(Preferences.Key.SYNC_SERVER_URI, uri);
				
				authenticate((String) serverUri);
				return true;
			}

		});
		
		// make sure to remove pw from persistent storage if user deselects it:
		persistPasswordPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean saveLocally = (Boolean)newValue;
				
				if (!saveLocally)
					Log.d(TAG, "removing pw from local storage");
				SecurityUtil sec = SecurityUtil.getInstance();
				Context context = getApplicationContext();
				byte[] password = sec.getPassword(context);
				// by saving again with the new settings it will be written to
				// temporary memory and removed from local
				Preferences.putBoolean(Preferences.Key.SAVE_PASSWORD, saveLocally);
				sec.setPassword(password);
				
				return true;
			}
		});
		
		syncSharesPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String selectedShare = (String)newValue;
				// did the selection change?
				if (ImportShare.removeShare(selectedShare))
					Toast.makeText(getApplicationContext(), "share successfully removed", Toast.LENGTH_LONG).show();
				else
					Toast.makeText(getApplicationContext(), "error removing share", Toast.LENGTH_LONG).show();
				// update the list
				fillSharesList();
				return true;
			}
		});

	}

	private void authenticate(String serverUri)
	{

		// update the value before doing anything
		Preferences.putString(Preferences.Key.SYNC_SERVER_URI, serverUri);

		SyncMethod currentSyncMethod = SyncManager.getInstance().getCurrentSyncMethod();

		if (!currentSyncMethod.needsAuth()) {
			return;
		}

		// service needs authentication
		Log.i(TAG, "Creating dialog");

		final ProgressDialog authProgress = ProgressDialog.show(this, "", "Authenticating. Please wait...", true, false);

		Handler handler = new Handler()
		{

			@Override
			public void handleMessage(Message msg)
			{

				boolean wasSuccsessful = false;
				Uri authorizationUri = (Uri) msg.obj;
				if (authorizationUri != null)
				{

					Intent i = new Intent(Intent.ACTION_VIEW, authorizationUri);
					startActivity(i);
					wasSuccsessful = true;

				} else
				{
					// Auth failed, don't update the value
					wasSuccsessful = false;
				}

				if (authProgress != null)
					authProgress.dismiss();

				if (wasSuccsessful)
				{
					resetLocalDatabase();
				} else
				{
					connectionFailed();
				}
			}
		};

		((ServiceAuth) currentSyncMethod).getAuthUri(serverUri, handler);
	}
	
	private void fillSharesList()
	{
		List<String> allShares = ImportShare.getAllShares();
		CharSequence[] entries = new CharSequence[allShares.size()];
		CharSequence[] entryValues = new CharSequence[allShares.size()];

		for (int i = 0; i < allShares.size(); i++)
		{
			entries[i] = allShares.get(i);
			entryValues[i] = allShares.get(i);
		}

		syncSharesPreference.setEntries(entries);
		syncSharesPreference.setEntryValues(entryValues);
	}

	private void fillSyncServicesList()
	{
		ArrayList<SyncMethod> availableServices = SyncManager.getInstance().getSyncMethods();
		CharSequence[] entries = new CharSequence[availableServices.size()];
		CharSequence[] entryValues = new CharSequence[availableServices.size()];

		for (int i = 0; i < availableServices.size(); i++)
		{
			entries[i] = availableServices.get(i).getDescription();
			entryValues[i] = availableServices.get(i).getName();
		}

		syncMethodPreference.setEntries(entries);
		syncMethodPreference.setEntryValues(entryValues);
	}

	private void setDefaults()
	{
		String defaultServer = (String)Preferences.Key.SYNC_SERVER_URI.getDefault();
		syncServerUriPreference.setDefaultValue(defaultServer);
		if(syncServerUriPreference.getText() == null)
			syncServerUriPreference.setText(defaultServer);

		boolean storePws = (Boolean)Preferences.Key.SAVE_PASSWORD.getDefault();
		persistPasswordPreference.setDefaultValue(storePws);
		
		String defaultService = (String)Preferences.Key.SYNC_METHOD.getDefault();
		syncMethodPreference.setDefaultValue(defaultService);
		if(syncMethodPreference.getValue() == null) {
			syncMethodPreference.setValue(defaultService);
			// also for store pws, because there we can't check for null
			persistPasswordPreference.setChecked(storePws);
		}		
	}

	private void updatePreferencesTo(String syncMethodName) {
		
		SyncMethod syncMethod = SyncManager.getInstance().getSyncMethod(syncMethodName);
	}

	private void setServer(String syncServiceKey)
	{

		SyncMethod syncMethod = SyncManager.getInstance().getSyncMethod(syncServiceKey);

		if (syncMethod == null)
			return;

		syncServerUriPreference.setEnabled(syncMethod.needsServer());
		syncMethodPreference.setSummary(syncMethod.getDescription());
	}

	private void connectionFailed()
	{
		new AlertDialog.Builder(this).setMessage(getString(R.string.prefSyncConnectionFailed)).setNeutralButton(getString(R.string.btnOk), new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		}).show();
	}

	// TODO use LocalStorage wrapper from two-way-sync branch when it get's
	// merged
	private void resetLocalDatabase()
	{
		getContentResolver().delete(Tomdroid.CONTENT_URI, null, null);
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, 0);

		// add a first explanatory note
		// TODO this will be problematic with two-way sync
		NoteManager.putNote(this, FirstNote.createFirstNote());
	}

	/**
	 * Housekeeping when a syncServer changes
	 * @param syncMethodKey - key of the new sync service 
	 */
	private void syncServiceChanged(String syncMethodKey) {
		
		updatePreferencesTo(syncMethodKey);

		// TODO this should be refactored further, notice that setServer performs
		// the same operations
		SyncMethod syncMethod = SyncManager.getInstance().getSyncMethod(
				syncMethodKey);

		if (syncMethod == null)
			return;

		// reset if no-auth required
		// I believe it's done this way because if needsAuth the database is reset when they successfully auth for the first time
		// TODO we should graphically warn the user that his database is about to be dropped
		if (!syncMethod.needsAuth()) {
		    resetLocalDatabase();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// we often used a menu item here to do some testing, if we don't need it, it is just not displayed
		boolean testing = false;
		if (testing) {
		// Create the menu based on what is defined in res/menu/main.xml
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_webdav, menu);
		return true;
		} else {
			return false;
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menuWebDAV:
			// currently there is no test code in here
			// TODO remove this completely when done
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}

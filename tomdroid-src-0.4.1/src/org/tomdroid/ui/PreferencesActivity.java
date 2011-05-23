/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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
package org.tomdroid.ui;

import java.util.ArrayList;

import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;
import org.tomdroid.sync.WebDAV.WebDAVSyncService;
import org.tomdroid.util.FirstNote;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity
{

	private static final String TAG = "PreferencesActivity";

	// TODO: put the various preferences in fields and figure out what to do on
	// activity suspend/resume
	private ListPreference syncService = null;

	// Username storage fields
	public static final String USERNAME_STORAGE = "username_storage";
	public static final String USERNAME_FIELD = "username_field";
	public static final String PASSW_STORAGE = "passw_storage";
	public static final String PASSW_FIELD = "passw_field";
	public static final String PATH_STORAGE = "path_storage";
	public static final String PATH_FIELD = "path_field";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// Fill the Preferences fields
		syncService = (ListPreference) findPreference(Preferences.Key.SYNC_SERVICE.getName());

		// Set the default values if nothing exists
		this.setDefaults();

		// Fill the services combo list
		this.fillServices();

		// Enable or disable the server field depending on the selected sync
		// service
		// setServer(syncService.getValue());

		syncService.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{

			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				String selectedSyncServiceKey = (String) newValue;

				// did the selection change?
				if (!syncService.getValue().contentEquals(selectedSyncServiceKey))
				{
					Log.d(TAG, "preference change triggered");

					syncServiceChanged(selectedSyncServiceKey);
				}

				if (selectedSyncServiceKey.equals("WebDAV"))
				{
					createLoginDialog();
				}
				return true;

			}
		});

	}

	private EditText usernameEdit;
	private EditText passwordEdit;
	private EditText pathEdit;

	/**
	 * creates the Login Dialog for the WebDAV login
	 */
	private void createLoginDialog()
	{
		final Dialog loginDialog = new Dialog(this);
		loginDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		loginDialog.setTitle("Enter Username and Password for WebDAV login:");

		LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View dialogView = li.inflate(R.layout.login_dialog, null);
		loginDialog.setContentView(dialogView);

		usernameEdit = (EditText) dialogView.findViewById(R.id.username_id);
		passwordEdit = (EditText) dialogView.findViewById(R.id.password_id);
		pathEdit = (EditText) dialogView.findViewById(R.id.path_id);

		String a = getUsernameStorage();
		String b = getPasswordStorage();
		if (getUsernameStorage() != null)
		{
			usernameEdit.setText(getUsernameStorage());
		}
		if (getPasswordStorage() != null)
		{
			passwordEdit.setText(getPasswordStorage());
		}
		if (getPathStorage() != null)
		{
			pathEdit.setText(getPathStorage());
		}

		loginDialog.show();

		Button okButton = (Button) dialogView.findViewById(R.id.ok_button);
		Button cancelButton = (Button) dialogView.findViewById(R.id.cancel_button);

		okButton.setOnClickListener(new View.OnClickListener()
		{
			// @Override
			public void onClick(View v)
			{
				// save username
				WebDAVSyncService.setUsername(usernameEdit.getText().toString());
				WebDAVSyncService.setPassword(passwordEdit.getText().toString());
				WebDAVSyncService.setPath(pathEdit.getText().toString());

				loginDialog.dismiss();
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener()
		{
			// @Override
			public void onClick(View v)
			{
				loginDialog.dismiss();
			}
		});

	}

	/**
	 * 
	 * @return Username from systemstorage
	 */
	public String getUsernameStorage()
	{
		SharedPreferences profileStored = getSharedPreferences(USERNAME_STORAGE, 0);

		try
		{
			return profileStored.getString(USERNAME_FIELD, "");

		} catch (Exception _ex)
		{
			return "";
		}
	}

	/**
	 * 
	 * @return Password from systemstorage
	 */
	public String getPasswordStorage()
	{
		SharedPreferences profileStored = getSharedPreferences(PASSW_STORAGE, 0);

		try
		{
			return profileStored.getString(PASSW_FIELD, "");

		} catch (Exception _ex)
		{
			return "";
		}
	}

	/**
	 * 
	 * @return path from systemstorage
	 */
	public String getPathStorage()
	{
		SharedPreferences profileStored = getSharedPreferences(PATH_STORAGE, 0);

		try
		{
			return profileStored.getString(PATH_FIELD, "");

		} catch (Exception _ex)
		{
			return "";
		}
	}

	private void fillServices()
	{
		ArrayList<SyncService> availableServices = SyncManager.getInstance().getServices();
		CharSequence[] entries = new CharSequence[availableServices.size()];
		CharSequence[] entryValues = new CharSequence[availableServices.size()];

		for (int i = 0; i < availableServices.size(); i++)
		{
			entries[i] = availableServices.get(i).getDescription();
			entryValues[i] = availableServices.get(i).getName();
		}

		syncService.setEntries(entries);
		syncService.setEntryValues(entryValues);
	}

	private void setDefaults()
	{

		String defaultService = (String) Preferences.Key.SYNC_SERVICE.getDefault();
		syncService.setDefaultValue(defaultService);
		if (syncService.getValue() == null)
			syncService.setValue(defaultService);

	}

	// private void setServer(String syncServiceKey)
	// {
	//
	// SyncService service =
	// SyncManager.getInstance().getService(syncServiceKey);
	//
	// if (service == null)
	// return;
	//
	// syncServer.setEnabled(service.needsServer());
	// syncService.setSummary(service.getDescription());
	// }

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
	 * 
	 * @param syncServiceKey
	 *           - key of the new sync service
	 */
	private void syncServiceChanged(String syncServiceKey)
	{
		// TODO this should be refactored further, notice that setServer performs
		// the same operations
		SyncService service = SyncManager.getInstance().getService(syncServiceKey);

		if (service == null)
			return;

		// reset if no-auth required
		// I believe it's done this way because if needsAuth the database is reset
		// when they successfully auth for the first time
		// TODO we should graphically warn the user that his database is about to
		// be dropped
		if (!service.needsAuth())
		{
			resetLocalDatabase();
		}
	}

	private static PreferencesActivity instance = null;

	public static PreferencesActivity getInstance()
	{
		if (instance == null)
			instance = new PreferencesActivity();

		return instance;
	}

}

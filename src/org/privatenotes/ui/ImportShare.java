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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.privatenotes.sync.WebDAV.SharedWebDAVSyncService;
import org.privatenotes.util.Preferences;
import org.privatenotes.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.widget.Toast;
import at.fhooe.mcm.webdav.WebDavFactory;

/**
 * Class that presents a GUI dialog where the user can enter a share-link that should
 * be imported
 */
public class ImportShare extends DialogWithInputBox
{

	// Logging info
	private static final String TAG = "ImportShare";
	private final String https = "https://";
	public static final String SHARELINKPREFIX = "note://tomboyshare/";
	public static final String CONFIGPREFIX = "note://synccfg/";
	
	private Activity last;
	private static ImportShare instance = new ImportShare();
	
	private ImportShare() {
	}
	
	/**
	 * shows an input-dialog where you can enter the new notes title
	 * @param last
	 */
	public static Dialog createNew(Activity last) {
		instance.last = last;
		return instance.createDialog(last, last.getString(R.string.shareAddUrl), "https://user:pw@host/path");
	}
	
	/**
	 * shows an input-dialog where you can enter the new notes title
	 * @param last
	 */
	public static void createNew(Activity last, String text) {
		instance.last = last;
		instance.showDialog(last, last.getString(R.string.shareAddUrl), text);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPositiveReaction(String text) {
		 // add path to shares:
		checkAndAddShare(text);
	}
	
	/**
	 * checks if we support that kind of share, if so adds it
	 * @param share the share link (url)
	 */
	public void checkAndAddShare(String share) {
		if (share.startsWith(SharedWebDAVSyncService.NOTESHARE_URL_PREFIX)) {
			if (Tomdroid.LOGGING_ENABLED)
				Log.i(TAG, "removing noteshare-prefix");
			share = share.substring(SharedWebDAVSyncService.NOTESHARE_URL_PREFIX.length());
		}
		
		if (share.trim().startsWith(https)) {
			if (WebDavFactory.getClientInfo().supportsHttps == false) {
				// ask the user if it's ok to switch to http
				dropHttpsOk(share);
				// return now, because that method already takes care of adding
				return;
			}
		}
		
		if (!share.startsWith("http://")) {
			onShareAdded(false, "unknown share-link format!");
			return;
		}
		
		storeShare(share);
	}
	
	/**
	 * stores the share into the internal storage if it doesn't already exist
	 * @param share
	 */
	private void storeShare(String share) {
		List<String> paths = getAllShares();
		if (paths.contains(share))
			onShareAdded(false, null);
		paths.add(share);
		Preferences.putString(Preferences.Key.SHARES, flattenShares(paths));
		onShareAdded(true, null);
	}
	
	/**
	 * reaction after the share has been processed
	 * @param success if true it is assumed that it was added, if false it was not added
	 * @param errorMsg if not null, that error message will be displayed instead (only for success = false)
	 */
	private void onShareAdded(boolean success, String errorMsg) {
		if (!success) {
			Toast.makeText(last.getApplicationContext(), (errorMsg != null) ? errorMsg : last.getString(R.string.shareAlreadyKnown), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(last.getApplicationContext(), last.getString(R.string.shareAdded), Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * asks the user if it's ok to switch from https to http
	 * @return
	 */
	private void dropHttpsOk(final String share) {
		new AlertDialog.Builder(last).setMessage("Cannot use https because of WebDav Library. Switch to http?").setTitle("Unsupported Protocol").
		setNeutralButton("Yes", new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
				String httpShare = "http://" + share.trim().substring(https.length());
				storeShare(httpShare);
			}
		}).setNegativeButton("No", new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				onShareAdded(false, "cannot add https share");
			}
		}).setIcon(R.drawable.icon).show();
	}
	
	/**
	 * removes a share from the internal storage of shares (the links, not
	 * the actual note is removed)
	 * @param share
	 * @return
	 */
	public static boolean removeShare(String share) {
		List<String> paths = getAllShares();
		if (!paths.contains(share))
			return false;
		paths.remove(share);
		Preferences.putString(Preferences.Key.SHARES, flattenShares(paths));
		return true;
	}
	
	/**
	 * get a list of all share-links that we currently have stored
	 * @return
	 */
	public static List<String> getAllShares() {
		String shares = Preferences.getString(Preferences.Key.SHARES);
		List<String> results = new ArrayList<String>();
		String[] arr = shares.split("\\n");
		for (String share : arr) {
			if (!"".equals(share.trim())) {
				results.add(share);
			}
		}
		return results;
	}
	
	/**
	 * flattens a list of shares to a string
	 * @param shares
	 * @return
	 */
	private static String flattenShares(List<String> shares) {
		StringBuffer sb = new StringBuffer();
		for (String p : shares) {
			if (sb.length() > 0)
				sb.append("\n");
			sb.append(p);
		}
		return sb.toString();
	}

}

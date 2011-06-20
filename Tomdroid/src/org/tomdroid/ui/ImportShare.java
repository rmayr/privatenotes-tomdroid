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
package org.tomdroid.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.tomdroid.R;
import org.tomdroid.sync.WebDAV.SharedWebDAVSyncService;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.widget.Toast;

public class ImportShare extends DialogWithInputBox
{

	// Logging info
	private static final String TAG = "ImportShare";
	
	private Activity last;
	private static ImportShare instance = new ImportShare();
	
	private ImportShare() {
	}
	
	/**
	 * shows an input-dialog where you can enter the new notes title
	 * @param last
	 */
	public static void createNew(Activity last) {
		instance.last = last;
		instance.showDialog(last, last.getString(R.string.shareAddUrl), "https://wampp:xampp@10.0.0.1/testuser3");
	}
	
	@Override
	protected void onPositiveReaction(String text) {
		 // add path to shares:
		if (!addShare(text)) {
			Toast.makeText(last.getApplicationContext(), last.getString(R.string.shareAlreadyKnown), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(last.getApplicationContext(), last.getString(R.string.shareAdded), Toast.LENGTH_LONG).show();
		}
	}
	
	public static boolean addShare(String share) {
		if (share.startsWith(SharedWebDAVSyncService.NOTESHARE_URL_PREFIX)) {
			share = share.substring(SharedWebDAVSyncService.NOTESHARE_URL_PREFIX.length());
		}
		
		List<String> paths = getAllShares();
		if (paths.contains(share))
			return false;
		paths.add(share);
		Preferences.putString(Preferences.Key.SHARES, flattenShares(paths));
		return true;
	}
	
	public static boolean removeShare(String share) {
		List<String> paths = getAllShares();
		if (!paths.contains(share))
			return false;
		paths.remove(share);
		Preferences.putString(Preferences.Key.SHARES, flattenShares(paths));
		return true;
	}
	
	public static List<String> getAllShares() {
		String shares = Preferences.getString(Preferences.Key.SHARES);
		return new ArrayList<String>(Arrays.asList(shares.split("\\n")));
	}
	
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

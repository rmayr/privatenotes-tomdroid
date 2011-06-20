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
package org.tomdroid.sync.sd;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.tomdroid.Note;
import org.tomdroid.sync.EncryptionException;
import org.tomdroid.sync.ShareHolder;
import org.tomdroid.sync.ShareHolder.Share;
import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

public class SdCardSyncServiceShared extends SdCardSyncService {
	
	protected ShareHolder shares;
	
	// logging related
	private final static String TAG = "SdCardSyncServiceShared";
	
	/** this is used for subclasses to get the information which notes have changed */
	protected List<Note> newAndUpdated = null;
	
	public SdCardSyncServiceShared(Activity activity, Handler handler) throws FileNotFoundException {
		super(activity, handler);
	}
	
	@Override
	public String getDescription() {
		return "SD Card Shared";
	}

	@Override
	public String getName() {
		return "sdcard_shared";
	}
	
	@Override
	protected void syncBody() {
		
		newAndUpdated = null;
		
		List<String> locations = shares.getAllLocations();
		List<File> additional = new ArrayList<File>();
		for (String location : locations) {
			Share share = shares.getByLocation(location);
			additional.add(new File (share.localPath));
		}
		
		
		
		SdSyncServer server;
		try {
			server = new SdSyncServerShared(path, additional, shares, localStorage.getNoteGuids());
			server.init();
		} catch (EncryptionException ee) {
			if (Tomdroid.LOGGING_ENABLED)
				Log.w(TAG, "encryption exception", ee);
			sendMessage(ENCRYPTION_ERROR);
			setSyncProgress(100);
			return;
		} catch (Exception e) {
			if (Tomdroid.LOGGING_ENABLED)
				Log.w(TAG, "error parsing ", e);
			sendMessage(PARSING_FAILED);
			setSyncProgress(100);
			return;
		}
		
		if (server.isInSync(localStorage)) {
			Log.d(TAG, "nothing new to sync");
			setSyncProgress(100);
			return;
		}
		
		ArrayList<Note> newFromServer;
		try {
			newFromServer = server.getNoteUpdates();
		} catch (Exception e) {
			if (Tomdroid.LOGGING_ENABLED) Log.i(TAG, "error acquiring notes from " + path, e);
			setSyncProgress(100);
			sendMessage(PARSING_FAILED);
			return;
		}
		
		setSyncProgress(50);
		
		
		insertAndUpdateLocalNotes(newFromServer);
		
		try {
			deleteNotesNotFoundOnServer(server);
		} catch (Exception e) {
			if (Tomdroid.LOGGING_ENABLED) Log.i(TAG, "error deleting notes locally", e);
			setSyncProgress(100);
			sendMessage(PARSING_FAILED);
			return;
		}
		
		try {
			deleteNotesNotFoundOnClient(server);
		} catch (Exception e) {
			if (Tomdroid.LOGGING_ENABLED) Log.i(TAG, "error deleting notes externally", e);
			setSyncProgress(100);
			sendMessage(PARSING_FAILED);
			return;
		}
		
		newAndUpdated = getLocalStorage().getNewAndUpdatedNotes();
		
		if (!server.createNewRevisionWith(newAndUpdated,
				getLocalStorage().getNoteGuids())) {
			setSyncProgress(100);
			return;
	    }
		setSyncProgress(90);


		getLocalStorage().onSynced(server.getSyncRevision());
		setSyncProgress(100);
	}

	private void insertAndUpdateLocalNotes(ArrayList<Note> serverUpdates) {
		for (Note noteUpdate : serverUpdates) {
			localStorage.mergeNote(noteUpdate);
		}
	}
	
	private void deleteNotesNotFoundOnServer(SdSyncServer server) throws Exception {
		// TODO
	}
	
	private void deleteNotesNotFoundOnClient(SdSyncServer server) throws Exception {
		// TODO
//		Set<String> locallyRemovedNoteIds = server.getNoteIds();
//		locallyRemovedNoteIds.removeAll(getLocalStorage().getNoteGuids());
//		server.delete(locallyRemovedNoteIds);
	}
	
}
	


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
import org.tomdroid.sync.LocalStorage;
import org.tomdroid.sync.SyncMethod;
import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

/**
 * sync service for the sd card which allows you to do a 2-way sync with a
 * folder on the sdcard, works together with SdSyncServer
 * @see SdSyncServer
 *
 */
public class SdCardSyncService extends SyncMethod {
	
	protected File path;
	protected LocalStorage localStorage;
	
	// logging related
	private final static String TAG = "SdCardSyncService";
	
	/** this is used for subclasses to get the information which notes have changed */
	protected List<Note> newAndUpdated = null;
	
	public SdCardSyncService(Activity activity, Handler handler) throws FileNotFoundException {
		super(activity, handler);
		
		path = new File(Tomdroid.NOTES_PATH);
		
		localStorage = new LocalStorage(activity);
		
		if (!path.exists())
			path.mkdir();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "SD Card";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "sdcard";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean needsServer() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean needsAuth() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void sync() {

		setSyncProgress(0);
			
		sync(true);
	}
	
	/**
	 * executes the sync process, this can either happen in a new thread or
	 * synchronously in the calling thread
	 * @param asynchronous if false execution will block the calling thread, if false
	 *   a new thread will be used for execution
	 */
	protected void sync(boolean asynchronous) {
		
		Runnable r = new Runnable() {
			public void run() {
				syncBody();
			}
		};

		if (asynchronous)
			execInThread(r);
		else
			r.run();
	}
	
	/**
	 * the actual sync work is done here
	 */
	protected void syncBody() {
		
		newAndUpdated = null;
		
		long localLastSync = localStorage.getLatestSyncVersion();
		
		SdSyncServer server;
		try {
			server = new SdSyncServer(path);
			server.init(localStorage);
		} catch (Exception e) {
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

	/**
	 * saves new notes from the server to the local repo
	 * @param serverUpdates
	 */
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
	
	/**
	 * cleans a directory for sync, this means deleting all .xml and .note files
	 * @param path the directory to clean
	 */
	public static void cleanDirectory(File path) {
		File[] files = path.listFiles();
		if (files == null)
			return;
		for (File f : files) {
			String fname = f.getName();
			if (fname.endsWith(".note") || fname.endsWith(".xml"))
				f.delete();
		}
	}
	
	/**
	 * cleans a directory for sync, removes all folders starting with "temp_"
	 * which are used by some syncs
	 * @param path the directory to clean
	 */
	public static void removeTempFolders(File path) {
		File[] files = path.listFiles();
		if (files == null)
			return;
		for (File f : files) {
			if (f.isDirectory()) {
				if (f.getName().startsWith("temp_")) {
					recursiveDelete(f);
				}
			}				
		}
	}
	
	/**
	 * recursively deletes directories + the directory itself
	 * @param dir
	 * @return
	 */
	public static boolean recursiveDelete(File dir) {
	    if (dir.isDirectory()) {
	        String[] children = dir.list();
	        for (int i=0; i<children.length; i++) {
	            boolean success = recursiveDelete(new File(dir, children[i]));
	            if (!success) {
	                return false;
	            }
	        }
	    }

	    // The directory is now empty so delete it
	    return dir.delete();
	}
	
}
	


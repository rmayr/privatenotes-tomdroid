/*
 * PrivateNotes
 * 
 * Copyright 2011 Paul Klingelhuber <paul.klingelhuber@students.fh-hagenberg.at>
 * 
 * This file is part of PrivateNotes.
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
package org.privatenotes.sync.WebDAV;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.Vector;

import org.privatenotes.Note;
import org.privatenotes.sync.NotesFilter;
import org.privatenotes.sync.ShareHolder;
import org.privatenotes.sync.sd.SdCardSyncServiceShared;
import org.privatenotes.ui.ImportShare;
import org.privatenotes.ui.Tomdroid;
import org.privatenotes.util.Preferences;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import at.fhooe.mcm.webdav.IWebDav;
import at.fhooe.mcm.webdav.WebDavFactory;

/**
 * An extension of the WebDAVSyncService which also supports shares.
 * This means there can be individual notes that are shared between several
 * instances of tomboy/tomdroid.
 *
 */
public class SharedWebDAVSyncService extends SdCardSyncServiceShared
{
	private static final String TAG = "WebDAVSharedSyncService";
	
	public static final String NOTESHARE_URL_PREFIX = "note://tomboyshare/";
	
	public SharedWebDAVSyncService(Activity activity, Handler handler) throws FileNotFoundException {
		super(activity, handler);
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "PrivateNotes";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "WebDav_Shared";
	}
	
	/**
	 * gets a map of all share-clients this means a webdav client
	 * for every share, the key is the share-url
	 * @return
	 */
	private Map<String, IWebDav> getAllShareClients() {
		List<String> shares = ImportShare.getAllShares();
		Map<String, IWebDav> results = new HashMap<String, IWebDav>();
		for (String share : shares) {
			try {
				results.put(share, WebDavFactory.getClient(share));
			} catch (Exception e) {
				Log.e(TAG, "could not get client for '" + share + "'", e);
				sendMessage(NO_INTERNET);
			}
		}
		return results;
	}
	
	/**
	 * downloads all necessary files from the webdav client to a certain
	 * directory
	 * @param client
	 * @param location target directory
	 * @return
	 */
	private List<String> downloadFilesTo(IWebDav client, File location) {
		int count = 0;
		Vector<String> children = client.getAllChildren();
		List<String> downloaded = new ArrayList<String>();
		for (String child : children) {
			if (!child.endsWith("/")) {
				// only download files, not directories
				client.download(child, new File(location, new File(child).getName()));
				downloaded.add(child);
			}
			count++;
			setSyncProgress(Math.min(30, count*5));
		}
		return downloaded;
	}
	
	/**
	 * creates a temporary subfolder
	 * @param base
	 * @return
	 */
	private File createTemporarySubDir(File base) {
		File temp = new File(base, "temp_" + UUID.randomUUID().toString());
		temp.mkdirs();
		return temp;
	}
	
	/**
	 * uploads a note to the right destination (normal notes to our main webdav folder,
	 * shared ones to the shared webdav destinations)
	 * @param defaultClient
	 * @param others
	 * @param noteId
	 * @param from
	 * @param to
	 * @return
	 */
	private boolean uploadNote(IWebDav defaultClient, Map<String, IWebDav> others, String noteId, File from, String to) {
		ShareHolder.Share share = shares.getByNote(noteId);
		if (share == null) {
			// upload to default location
			return defaultClient.upload(from, to);
		} else {
			IWebDav client = others.get(share.location);
			if (client == null)
				throw new InvalidParameterException("no client for location " + share.location);
			
			// we also have to upload the corresponding manifest files
			// since we currently only support 1note per share-location
			// we do this here... it's the easiest way
			client.upload(new File(share.localPath, "manifest.xml"), "manifest.xml");
			
			return client.upload(from, to);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void sync() {
		if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "beginning webdav sync");
		
		// check if we have the necessary passwords
		if (Tomdroid.getInstance().showPasswordEntryIfNecessary()) {
			//setSyncProgress(100);
			//sendMessage(ENCRYPTION_ERROR);
			return;
		}
		
		final File path = new File(Tomdroid.NOTES_PATH);
		
		if (!path.exists()) {
			path.mkdir();
		} else {
			// path exists, clean the directory:
			cleanDirectory(path);
			
			for (String name : path.list())
				Log.w(TAG, "files_remained: " + name);
			
		}
		
		
		setSyncProgress(0);
		
		execInThread(new Runnable() {
			public void run() {
				sharedWebdavSyncbody( path);
			}
		});

	}
	
	/**
	 * actual sync work 
	 * @param path
	 */
	protected void sharedWebdavSyncbody(File path) {
		IWebDav wdc = null;
		
		try {
			String serverUri = Preferences.getString(Preferences.Key.SYNC_SERVER_URI);
			wdc = WebDavFactory.getClient(serverUri);	
		} catch (Exception e) {
			Log.e(TAG, "error @ sync", e);
			sendMessage(NO_INTERNET);
			setSyncProgress(100);
			return;
		}
		
		try {
			int count = 0;
			shares = new ShareHolder();
			
			List<String> children = new ArrayList<String>(); // all the "children" of the webdav folders
			
			Map<String, IWebDav> clients = getAllShareClients();
			
			for (Entry<String, IWebDav> client : clients.entrySet()) {
				File clientDir = createTemporarySubDir(path);
				List<String> found = downloadFilesTo(client.getValue(), clientDir);
				children.addAll(found);
				count += found.size();
				
				for (String file : found) {
					if (file.endsWith(".note")) {
						// we simply move all note files to the main dir
						File inBase = new File(path, file);
						File inTempFolder = new File(clientDir, file); 
						inTempFolder.renameTo(inBase);
						
						// foreign manifests are read out later, this will then
						// update the information stored into ShareHolder object
						String noteId = file.replace(".note", "");
						List<String> partners = new ArrayList<String>();
						shares.addShare(client.getKey(), clientDir.getAbsolutePath(), noteId, partners);
					}
				}
			}
			
			Log.d(TAG, "downloading all files from webdav");
			List<String> found = downloadFilesTo(wdc, path);
			children.addAll(found);
			count += found.size();
			
			Log.d(TAG, "download complete");
			
			// we have to do it in a synchronous way
			super.sync(false);
			
			// check which ones have changed
			List<Note> changed = (newAndUpdated == null) ? new ArrayList<Note>() : newAndUpdated;
			List<String> changedWhileSync = new ArrayList<String>();
			for (Note n : changed) {
				String fileName = n.getGuid().toString() + ".note";
				changedWhileSync.add(fileName);
			}
			
			// if the notes have changed, the manifest needs to be updated as well
			if (changedWhileSync.size() > 0)
				changedWhileSync.add("manifest.xml");
			
			// now upload
			Log.d(TAG, "uploading new and updated files to webdav");
			String[] files = path.list(new NotesFilter());
			for (String file : files) {
				if (!children.contains(file)) {
					// this file is not already on webdav -> upload it
					Log.d(TAG, "uploading " + file + " because it wasn't on webdav");
					uploadNote(wdc, clients, file.replace(".note", ""), new File(path, file), file);
				} else if(changedWhileSync.contains(file)) {
					Log.d(TAG, "uploading " + file + " because it has changed");
					uploadNote(wdc, clients, file.replace(".note", ""), new File(path, file), file);
				} else {
					// don't upload
				}
			}
			uploadNote(wdc, null, "manifest.xml", new File(path, "manifest.xml"), "manifest.xml");
			Log.d(TAG, "uploading done");
			
		
		} catch(Exception e) {
			Log.e(TAG, "error @ sync", e);
			sendMessage(NO_INTERNET);
		} finally {
			// delete all the temp folders:
			Log.w(TAG, "deleting sync files from cache");
			// TODO re-enable this when done testing
			removeTempFolders(path);
			cleanDirectory(path);
		}
	}

}

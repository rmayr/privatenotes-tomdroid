package org.tomdroid.sync.WebDAV;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.tomdroid.Note;
import org.tomdroid.sync.sd.SdCardSyncService;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import at.fhooe.mcm.webdav.IWebDav;
import at.fhooe.mcm.webdav.WebDavFactory;

public class WebDAVSyncService extends SdCardSyncService
{
	private static final String TAG = "WebDAVSyncService";
	//private IWebDav wdc;
	
	public WebDAVSyncService(Activity activity, Handler handler) throws FileNotFoundException {
		super(activity, handler);
		
	}
	
	@Override
	public String getDescription() {
		return "WebDav sync";
	}

	@Override
	public String getName() {
		return "WebDav";
	}

	@Override
	public boolean needsServer() {
		return true;
	}
	
	@Override
	protected void sync() {
		if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "beginning webdav sync");
		
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
		try {
			String serverUri = Preferences.getString(Preferences.Key.SYNC_SERVER_URI);
			final IWebDav wdc = WebDavFactory.getClient(serverUri);
			
			execInThread(new Runnable() {
				public void run() {
					syncbody(wdc, path);
				}
			});
		
		} catch(Exception _e) {
			Log.e(TAG, "error @ sync", _e);
			setSyncProgress(100);
			sendMessage(NO_INTERNET);
		}
	}
	
	protected void syncbody(IWebDav wdc, File path) {
		try {
			int count = 0;
			
			Log.d(TAG, "downloading all files from webdav");
			Vector<String> children = wdc.getAllChildren();
			for (String child : children) {
				if (!child.endsWith("/")) {
					// only download files, not directories
					wdc.download(child, new File(path, new File(child).getName()));
				}
				count++;
				setSyncProgress(Math.min(30, count*5));
			}
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
			
			// now upload
			Log.d(TAG, "uploading new and updated files to webdav");
			String[] files = path.list();
			for (String file : files) {
				if (!children.contains(file)) {
					// this file is not already on webdav -> upload it
					Log.d(TAG, "uploading " + file + " because it wasn't on webdav");
					wdc.upload(new File(path, file), file);
				} else if(changedWhileSync.contains(file)) {
					Log.d(TAG, "uploading " + file + " because it has changed");
					wdc.upload(new File(path, file), file);
				} else {
					// don't upload
				}
			}
			Log.d(TAG, "uploading done");
			
		
		} catch(Exception e) {
			Log.e(TAG, "error @ sync", e);
			sendMessage(NO_INTERNET);
		}
	}

}

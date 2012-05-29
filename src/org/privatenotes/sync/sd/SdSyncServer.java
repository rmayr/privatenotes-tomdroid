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
package org.privatenotes.sync.sd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.privatenotes.Note;
import org.privatenotes.sync.EncryptionException;
import org.privatenotes.sync.LocalStorage;
import org.privatenotes.sync.NotesFilter;
import org.privatenotes.ui.Tomdroid;
import org.privatenotes.util.Preferences;
import org.privatenotes.util.SecurityUtil;
import org.privatenotes.xml.NoteXmlSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.util.Log;
import android.util.TimeFormatException;
import at.fhooe.mcm.tomboyCrypt.CryptoScheme;
import at.fhooe.mcm.tomboyCrypt.CryptoSchemeProvider;

/**
 * sync server for the sd card which allows you to do a 2-way sync with a
 * folder on the sdcard
 *
 */
public class SdSyncServer {
	private static final String TAG = "SdSyncServer";

	protected static boolean NOTE_ENCRYPED = true;

	/** basepath (root folder) of tomboy sd-card files */
	protected File path;
	/** the manifest file */
	protected File manifestFile;
	public static final String MANIFESTFILENAME = "manifest.xml";
	protected Map<String, Integer> notesWithRevs;
	protected long syncVersionOnServer = -3;
	protected CryptoScheme cscheme = null;
	
	protected String serverId = "";

	// regexp for <note-content..>...</note-content>
	private static Pattern note_content = Pattern.compile(
			"<note-content.*>(.*)<\\/note-content>", Pattern.CASE_INSENSITIVE
					+ Pattern.DOTALL);

	public SdSyncServer(File path) {
		this.path = path;
	}
	
	/**
	 * this has to be called that the SyncServer retrieves all its info
	 */
	public void init(LocalStorage localStorage) throws Exception {
		manifestFile = path.getAbsoluteFile();
		manifestFile = new File(manifestFile, MANIFESTFILENAME);

		cscheme = CryptoSchemeProvider.getConfiguredCryptoScheme();
		
		notesWithRevs = new Hashtable<String, Integer>();

		boolean success = readManifestFile(notesWithRevs, manifestFile);

		if (!success)
			throw new Exception("cannot read xml file");
		
		if ("".equals(this.serverId)) {
			// there was no server-id, no manifest was there, create a new server-id
			this.serverId = UUID.randomUUID().toString();
		}
		else
		{
			// check if it's still the same server-id
			String lastServerId = Preferences.getString(Preferences.Key.SYNC_SERVER_UUID);
			if (!lastServerId.trim().equalsIgnoreCase(this.serverId.trim())) {
				Log.i(TAG, "Complete resync, deleting all local notes");
				// the server has changed, we completely reset our local data-base (delte all notes)
				localStorage.resetDatabase();
				// we put -2 because we get -1 if there is no sync data on the server
				// but then we still want to sync, and for that the numbers must not match
				Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, -2);
				
				// and save the new server-id:
				Preferences.putString(Preferences.Key.SYNC_SERVER_UUID, this.serverId.trim());
			}
		}

		for (Integer i : notesWithRevs.values()) {
			if (i.intValue() > syncVersionOnServer) {
				syncVersionOnServer = i;
			}
		}
		Log.d(TAG, "got info from sd card sync server, version is: "
				+ syncVersionOnServer);
	}

	/**
	 * reads all the new notes found on the sync server (sdcard) and returns
	 * them as note objects
	 * @return
	 * @throws FileNotFoundException
	 * @throws Exception
	 */
	public ArrayList<Note> getNoteUpdates() throws FileNotFoundException,
			Exception {
		ArrayList<Note> updates = new ArrayList<Note>();

		File[] fileList = path.listFiles(new NotesFilter());
		// it's not an error if there are no notes	
		if (fileList == null) {
			fileList = new File[0];
		}
		
		Map<String, File> idsWithFiles = new Hashtable<String, File>();
		for (File f : fileList) {
			idsWithFiles.put(f.getName().replace(".note", ""), f);
		}

		List<String> notesToBeFetched = getUpdatedNoteIds();

		int filesRead = 0;		
		for (String s : notesToBeFetched) {
			if (idsWithFiles.containsKey(s)) {
				File noteFile = idsWithFiles.get(s);
				Worker w = new Worker(noteFile);
				w.run();
				Note n = w.getNote();
				if (n != null) {
					updates.add(n);
					filesRead++;
				} else {
					Log.w(TAG, "Error parsing note " + noteFile.getAbsolutePath());
				}
			} else {
				Log.w(TAG, "No notefile with id " + s);
			}
		}

		return updates;
	}
	
	/**
	 * assembles a list of all note-ids that have been updated on the server
	 * and need to be fetched
	 * @return
	 */
	protected List<String> getUpdatedNoteIds() {
		List<String> results = new ArrayList<String>();

		long since = Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION);
		for (Map.Entry<String, Integer> entry : notesWithRevs.entrySet()) {
			if (entry.getValue() > since) {
				results.add(entry.getKey());
			}
		}
		return results;
	}

	/**
	 * returns true when there is nothing new to sync
	 * @param localStorage
	 * @return
	 */
	public boolean isInSync(LocalStorage localStorage) {
		return localStorage.getLatestSyncVersion() == syncVersionOnServer
				&& localStorage.getNewAndUpdatedNotes().isEmpty();
	}

	/**
	 * returns the current version of the notes on the server
	 * @return
	 */
	public Long getSyncRevision() {
		return syncVersionOnServer;
	}

	/**
	 * @return true if successful
	 */
	public boolean createNewRevisionWith(List<Note> newAndUpdatedNotes, Set<String> allNoteIds) {
		if (newAndUpdatedNotes.isEmpty()) {
			return true;
		}

		serializeNotes(newAndUpdatedNotes);
		long oldRevision = Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION);
		long newRevision = oldRevision;
		
		// the updated notes should have the same revision now anyway
		//if (newRevision >= getSyncRevision()) {
		if (true) { // can't figure out the right condition @ the moment
			newRevision = getSyncRevision() + 1;
			for (Note n : newAndUpdatedNotes) {
				n.setLastSyncRevision(newRevision + 1);
			}
		}
		
		// get remaining notes for manifest:
		ArrayList<String> remainingIds = new ArrayList<String>();
		remainingIds.addAll(allNoteIds);
		for (Note n : newAndUpdatedNotes) {
			remainingIds.remove(n.getGuid().toString());
		}

		if (!createManifest(newAndUpdatedNotes, newRevision, remainingIds, oldRevision, notesWithRevs)) {
			Log.w(TAG, "cannot create manifest");
			return false;
		}

		//if (newRevision == getSyncRevision() + 1) {
		if (newRevision == getSyncRevision() + 1) {
			syncVersionOnServer = newRevision;
			return true;
		}

		return false;
	}

	/**
	 * reads the manifest file and returns the note-ids and revs that are stored
	 * in it.
	 * @param notesWithRevs
	 * @param manifestFile the file to read
	 * @return
	 * @throws EncryptionException
	 */
	protected boolean readManifestFile(Map<String, Integer> notesWithRevs,
			File manifestFile) throws EncryptionException {
		
		if (!manifestFile.exists()) {
			// this is not an error, because when syncing the first time
			// the file simply doesn't exist yet
			Log.i(TAG, "there is no manifest xml file.");
			// create a new server-id
			this.serverId = "";
			return true;
		}

		byte[] decrypted = cscheme.decryptFile(manifestFile, SecurityUtil
				.getInstance().getPassword());
		// If a wrong password was enterd, a note will be created with the
		// content ERROR in decryption.
		if (decrypted == null) {
			throw new EncryptionException("crypto error while reading manifest");
		}
		String manifestText = new String(decrypted);

		try {
			// Parsing XML
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();

			// Get the XMLReader of the SAXParser we created
			XMLReader xr = sp.getXMLReader();

			// Create a new ContentHandler, send it this note to fill and apply
			// it to the XML-Reader
			ManifestHandler manifestHandler = new ManifestHandler(notesWithRevs);
			xr.setContentHandler(manifestHandler);

			xr.parse(new InputSource(new StringReader(manifestText)));
			
			this.serverId = manifestHandler.getServerId();
			return true;
		} catch (Exception _e) {
			Log.w(TAG, "parsing failed: " + _e.getMessage());
			return false;
		}
	}

	//
	// // PRIVATE STUFF
	//
	private void serializeNotes(List<Note> notes) {
		NoteXmlSerializer serializer = new NoteXmlSerializer();
		long biggestRev = -1;
		for (Note note : notes) {
			biggestRev = (biggestRev<note.getLastSyncRevision())?note.getLastSyncRevision():biggestRev;
			try {
				String data = serializer.serialize(note);
				// write 2 file
				byte[] key = SecurityUtil.getInstance().getPassword();
				boolean success = cscheme.writeFile(new File(path, note.getGuid().toString() + ".note"), data.getBytes(), key);
				
				if (!success)
					throw new EncryptionException("could not write serialized note");
				
			} catch (Exception _e) {
				// cannot serialize!
				Log.w(TAG, String.format("cannot serialize note '{0}'", note.getTags()), _e);
			}
		}
		// no more returning, because the rev stored within the notes is wrong!
		//return biggestRev;
	}

	private boolean createManifest(List<Note> newandupdated, long newRev, ArrayList<String> remainingIds, long oldRev, Map<String, Integer> revs) {
		ManifestXmlWriter mw = new ManifestXmlWriter();
		// TODO get server guid! HOWTO?!
		try {
			String data = mw.serialize(newandupdated, this.serverId, newRev, remainingIds, oldRev, revs);
			byte[] key = SecurityUtil.getInstance().getPassword();
			cscheme.writeFile(manifestFile, data.getBytes(), key);
		} catch (Exception _e) {
			Log.w(TAG, "cannot serialize manifest: " + _e.getMessage(), _e);
			return false;
		}
		return true;
	}
	
	/**
	 * used to get the contents of a file as specified by this server
	 * @param f
	 * @return
	 */
	protected byte[] getFileContents(File file) {
		byte[] decrypted = cscheme.decryptFile(file, SecurityUtil.getInstance().getPassword());
		return decrypted;
	}

	/**
	 * The worker spawns a new note, parse the file its being given by the
	 * executor.
	 */
	class Worker implements Runnable {

		// the note to be loaded and parsed
		private Note note = new Note();
		private File file;
		final char[] buffer = new char[0x10000];

		public Worker(File f) {
			file = f;
		}

		public Note getNote() {
			return note;
		}

		public void run() {
			/**
			 * DECRYPT
			 */
			byte[] decrypted = getFileContents(file);
			String noteText = null;
			// If a wrong password was enterd, a note will be created with the
			// content ERROR in decryption.
			if (decrypted == null) {
				Log.w(TAG, "could not deserialize note!");
				
				noteText = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
						+ "<note version=\"0.1\" xmlns:link=\"http://beatniksoftware.com/tomboy/link\" xmlns:size=\"http://beatniksoftware.com/tomboy/size\" xmlns=\"http://beatniksoftware.com/tomboy\">"
						+ "<title>ERROR in decryption</title>"
						+ "<text xml:space=\"preserve\"><note-content version=\"0.1\">wrong password entered</note-content></text>"
						+ "<last-change-date>2011-01-27T11:04:26.2892499+01:00</last-change-date>"
						+ "<last-metadata-change-date>2011-01-27T11:04:26.2892499+01:00</last-metadata-change-date>"
						+ "<create-date>2010-11-18T15:31:15.0012854+01:00</create-date>"
						+ "<cursor-position>326</cursor-position>"
						+ "<width>477</width>" + "<height>408</height>"
						+ "<x>1897</x>" + "<y>298</y>"
						+ "<open-on-startup>False</open-on-startup>"
						+ "</note>";
			} else {
				noteText = new String(decrypted);
			}

			InputSource is2 = new InputSource(new StringReader(noteText));

			note.setFileName(file.getAbsolutePath());
			// the note guid is not stored in the xml but in the filename
			note.setGuid(file.getName().replace(".note", ""));

			try {
				// Parsing
				// XML
				// Get a SAXParser from the SAXPArserFactory
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();

				// Get the XMLReader of the SAXParser we created
				XMLReader xr = sp.getXMLReader();

				// Create a new ContentHandler, send it this note to fill and
				// apply it to the XML-Reader
				NoteHandler xmlHandler = new NoteHandler(note);
				xr.setContentHandler(xmlHandler);

				if (Tomdroid.LOGGING_ENABLED)
					Log.d(TAG, "parsing note");

				if (NOTE_ENCRYPED) {
					xr.parse(is2);
				} else {
					// Create the proper input source
					FileInputStream fin = new FileInputStream(file);
					BufferedReader in = new BufferedReader(
							new InputStreamReader(fin), 8192);
					InputSource is = new InputSource(in);

					xr.parse(is);
				}

				// TODO wrap and throw a new exception here
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (TimeFormatException e) {
				e.printStackTrace();
				Log.e(TAG, "Problem parsing the note's date and time");
				note = null;
				return;
			}

			// extract the <note-content..>...</note-content>
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "retrieving what is inside of note-content");

			// FIXME here we are re-reading the whole note just to grab
			// note-content out, there is probably a best way to do this (I'm
			// talking to you xmlpull.org!)
			StringBuilder out = new StringBuilder();
			try {
				int read;
				Reader reader = null;

				if (NOTE_ENCRYPED) {
					reader = new StringReader(noteText);
				} else {
					reader = new InputStreamReader(new FileInputStream(file),
							"UTF-8");
				}

				do {
					read = reader.read(buffer, 0, buffer.length);
					if (read > 0) {
						out.append(buffer, 0, read);
					}
				} while (read >= 0);

				Matcher m = note_content.matcher(out.toString());
				if (m.find()) {
					note.setXmlContent(m.group());
				} else {
					if (Tomdroid.LOGGING_ENABLED)
						Log
								.w(TAG,
										"Something went wrong trying to grab the note-content out of a note");
				}

			} catch (IOException e) {
				// TODO handle properly
				e.printStackTrace();
				if (Tomdroid.LOGGING_ENABLED)
					Log.w(TAG, "Something went wrong trying to read the note");
			}
		}

	}

}

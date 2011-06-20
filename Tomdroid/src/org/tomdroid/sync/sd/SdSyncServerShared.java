package org.tomdroid.sync.sd;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.Note;
import org.tomdroid.sync.EncryptionException;
import org.tomdroid.sync.LocalStorage;
import org.tomdroid.sync.ShareHolder;
import org.tomdroid.sync.ShareHolder.Share;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.SecurityUtil;
import org.tomdroid.xml.NoteXmlSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.util.Log;
import at.fhooe.mcm.tomboyCrypt.AsymmetricCryptoScheme;

public class SdSyncServerShared extends SdSyncServer {
	private static final String TAG = "SdSyncServerShared";
	
	protected List<File> otherDirs;
	private ShareHolder shares;
	private Set<String> allCurrentNoteIDs;

	public SdSyncServerShared(File path, List<File> additionalDirectories, ShareHolder shareInfos, Set<String> allNoteIDs) {
		super(path);
		otherDirs = additionalDirectories;
		shares = shareInfos;
		allCurrentNoteIDs = allNoteIDs;
	}

	/**
	 * @return true if successful
	 */
	// TODO fix this for shared stuff
	@Override
	public boolean createNewRevisionWith(List<Note> newAndUpdatedNotes, Set<String> allNoteIds) {
		if (newAndUpdatedNotes.isEmpty()) {
			return true;
		}

		serializeNotes(newAndUpdatedNotes);
		long oldRevision = Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION);
		oldRevision = (oldRevision>=0) ? oldRevision : 0; // must be at least 0
		long newRevision = oldRevision;
		
		// the updated notes should have the same revision now anyway
		//if (newRevision >= getSyncRevision()) {
		if (true) { // can't figure out the right condition @ the moment
			newRevision = getSyncRevision() + 1;
			newRevision = (newRevision>=0) ? newRevision : 0;
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

		if (!createManifest(newAndUpdatedNotes, newRevision, remainingIds, oldRevision)) {
			Log.w(TAG, "cannot create manifest");
			return false;
		}

		// fix for detecting first sync
		syncVersionOnServer = (syncVersionOnServer < 0) ? 0 : syncVersionOnServer;
		if (newRevision == getSyncRevision() + 1) {
			syncVersionOnServer = newRevision;
			return true;
		}

		return false;
	}
	
	/**
	 * for the shared sync we also check if we already
	 * have all notes, because it could be that a shared note has a lower version number
	 */
	public boolean isInSync(LocalStorage localStorage) {
		Set<String> allUUIDs = localStorage.getNoteGuids();
		boolean haveAll = true;
		Set<String> rest = new HashSet<String>(notesWithRevs.keySet());
		rest.removeAll(allUUIDs);
		haveAll = rest.size() == 0;
		
		return haveAll && localStorage.getLatestSyncVersion() == syncVersionOnServer
				&& localStorage.getNewAndUpdatedNotes().isEmpty();
	}
	
	/**
	 * {@inheritDoc}
	 * for shared, we also add those that we don't have already
	 */
	@Override
	protected List<String> getUpdatedNoteIds() {
		List<String> results = super.getUpdatedNoteIds();
		
		Set<String> rest = new HashSet<String>(notesWithRevs.keySet());
		rest.removeAll(allCurrentNoteIDs);
		
		results.addAll(rest);
		return results;
	}

	@Override
	protected boolean readManifestFile(Map<String, Integer> notesWithRevs,
			File manifestFile) throws EncryptionException {
		
		if (!manifestFile.exists()) {
			// this is not an error, because when syncing the first time
			// the file simply doesn't exist yet
			Log.i(TAG, "there is no manifest xml file.");
			return true;
		}

		byte[] decrypted = cscheme.decryptFile(manifestFile, SecurityUtil
				.getInstance().getPassword().getBytes());
		// If a wrong password was enterd, a note will be created with the
		// content ERROR in decryption.
		if (decrypted == null) {
			throw new EncryptionException("crypto error while reading manifest");
		}
		String manifestText = new String(decrypted);

		boolean success = parseManifest(manifestText, false);
		
		// now all the shared manifests:
		for (File f : otherDirs) {
			File sharedManifest = new File(f, "manifest.xml");
			if (sharedManifest.exists()) {
				decrypted = ((AsymmetricCryptoScheme)cscheme).decryptAsymFile(sharedManifest, SecurityUtil
						.getInstance().getPassword().getBytes());
				// If a wrong password was enterd, a note will be created with the
				// content ERROR in decryption.
				if (decrypted == null) {
					Log.w(TAG, "crypto error while reading a shared manifest");
					return false;
				}
				manifestText = new String(decrypted);
				parseManifest(manifestText, true);
			}
		}
		
		// TODO currently we only return the info about the "main" manifest... is this ok?
		return success;
	}
	
	/**
	 * 
	 * @param content the xml to parse
	 * @param isShared if true, the shared manifest parser will be used and
	 * 			the shareHolder object will be updated with share-partners
	 * @return
	 */
	private boolean parseManifest(String content, boolean isShared) {
		try {
			// Parsing XML
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();

			// Get the XMLReader of the SAXParser we created
			XMLReader xr = sp.getXMLReader();

			// Create a new ContentHandler, send it this note to fill and apply
			// it to the XML-Reader
			ManifestHandler manifestHandler = null;
			if (!isShared) {
				manifestHandler = new ManifestHandler(notesWithRevs);
			} else {
				manifestHandler = new SharedManifestHandler(notesWithRevs);
			}
			xr.setContentHandler(manifestHandler);

			xr.parse(new InputSource(new StringReader(content)));
			
			if (isShared) {
				// update the share-partners list
				SharedManifestHandler handler = (SharedManifestHandler)manifestHandler;
				List<String> ids = handler.getNoteIds();
				List<String> partners = handler.getSharedWith();
				for (String noteId : ids) {
					Share s = shares.getByNote(noteId);
					s.sharers.addAll(partners);
				}
			}
			
			return true;
		} catch (Exception _e) {
			Log.w(TAG, "parsing failed: " + _e.getMessage());
			return false;
		}
	}

	private void serializeNotes(List<Note> notes) {
		NoteXmlSerializer serializer = new NoteXmlSerializer();
		long biggestRev = -1;
		for (Note note : notes) {
			biggestRev = (biggestRev<note.getLastSyncRevision())?note.getLastSyncRevision():biggestRev;
			try {
				String data = serializer.serialize(note);
				// write 2 file
				byte[] key = SecurityUtil.getInstance().getPassword().getBytes();
				Share shareInfo = shares.getByNote(note.getGuid().toString());
				if (shares.getByNote(note.getGuid().toString()) == null) {
					cscheme.writeFile(new File(path, note.getGuid().toString() + ".note"), data.getBytes(), key);
				} else {
					// a shared note:
					((AsymmetricCryptoScheme)cscheme).writeAsymFile(new File(path, note.getGuid().toString() + ".note"), data.getBytes(), key, shareInfo.sharers);
				}
				
			} catch (Exception _e) {
				// cannot serialize!
				Log.w(TAG, String.format("cannot serialize note '{0}'", note.getTags()));
			}
		}
		// no more returning, because the rev stored within the notes is wrong!
		//return biggestRev;
	}

	/**
	 * 
	 * @param newandupdated
	 * @param newRev
	 * @param remainingIds
	 * @param oldRev
	 * @param destination the file where to write to
	 * @param sharedWith if != null, then a shared manifest will be created and also shared-encrypted
	 * @return
	 */
	private boolean createManifest(List<Note> newandupdated, long newRev, List<String> remainingIds, long oldRev) {
		byte[] key = SecurityUtil.getInstance().getPassword().getBytes();
		
		// new lists which will only contain notes/ids which are to be
		// synced via the normal sync (not shared)
		List<Note> newandupdatedNormal = new ArrayList<Note>(newandupdated);
		List<String> remainingIdsNormal = new ArrayList<String>(remainingIds);
		
		SharedManifestXmlWriter smw = new SharedManifestXmlWriter();
		// write shared manifests:
		Map<String, Note> updatedIds = new HashMap<String, Note>();
		for (Note n : newandupdated) {
			updatedIds.put(n.getGuid().toString(), n);
		}
		List<String> locations = shares.getAllLocations();
		List<String> emptyList = new ArrayList<String>();
		for (String location : locations) {
			Share s = shares.getByLocation(location);
			if (remainingIds.contains(s.noteId)) {
				// shared notes don't get synced to the noral repo
				remainingIdsNormal.remove(s.noteId);
			}
			if (updatedIds.containsKey(s.noteId)) {
				List<Note> notes = new ArrayList<Note>();
				Note note = updatedIds.get(s.noteId);
				notes.add(note);
				// shared notes don't get synced to the noral repo
				newandupdatedNormal.remove(note);
				try {
					String data = smw.serialize(notes, "TODO", newRev, emptyList, oldRev, notesWithRevs, s.sharers);
					File destination = new File(s.localPath, "manifest.xml");
					((AsymmetricCryptoScheme)cscheme).writeAsymFile(destination, data.getBytes(), key, s.sharers);
				} catch (Exception e) {
					Log.w(TAG, "error while writing shared manifest", e);
				}
			}
		}
		
		// now write the "normal" manifest
		ManifestXmlWriter mw = new ManifestXmlWriter();
		// TODO get server guid! HOWTO?!
		try {
			String data = mw.serialize(newandupdatedNormal, "TODO", newRev, remainingIdsNormal, oldRev, notesWithRevs);
			return cscheme.writeFile(manifestFile, data.getBytes(), key);
		} catch (Exception _e) {
			Log.w(TAG, "cannot serialize manifest: ", _e);
			return false;
		}
	}
	
	/**
	 * used to get the contents of a file as specified by this server
	 * 
	 * if its a shared file, we have to use shared crypto
	 * 
	 * @param f
	 * @return
	 */
	@Override
	protected byte[] getFileContents(File file) {
		String name = file.getName();
		byte[] key = SecurityUtil.getInstance().getPassword().getBytes();
		if (name.endsWith(".note")) {
			String noteId = name.replace(".note", "");
			Share s = shares.getByNote(noteId);
			if (s != null) {
				return ((AsymmetricCryptoScheme)cscheme).decryptAsymFile(file, key);
			} else {
				return cscheme.decryptFile(file, key);
			}
		} else {
			String errMsg = "This is note a note file, this method is only designed to handle such. " + file.getAbsolutePath();
			Log.w(TAG, errMsg);
			throw new UnsupportedOperationException(errMsg);
		}
	}

}

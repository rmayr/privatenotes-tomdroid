package org.tomdroid.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * data structure for holding multiple shares
 * a share consists of the note that it holds (we currently only support one -> TODO)
 * the people with whom it is shared and the locations of where it is stored on the
 * network and locally cached/downloaded
 * @author Paul Klingelhuber
 */
public class ShareHolder {
	public class Share {
		public String location;
		public String localPath;
		public String noteId;
		public List<String> sharers;
	}
	
	private Map<String, Share> entries;
	public ShareHolder() {
		entries = new HashMap<String, ShareHolder.Share>();
	}
	
	public List<String> getAllLocations() {
		return new ArrayList<String>(entries.keySet());
	}
	
	public Share getByLocation(String location) {
		return entries.get(location);
	}
	
	public Share getByNote(String noteId) {
		for (Entry<String, Share> elem : entries.entrySet()) {
			if (elem.getValue().noteId.equals(noteId)) {
				return elem.getValue();
			}
		}
		return null;
	}
	
	public void addShare(String location, String path, String noteId, List<String> partners) {
		if (entries.containsKey(location)) {
			Share s = entries.get(location);
			s.location = location;
			s.localPath = path;
			s.noteId = noteId;
			s.sharers = partners;
		} else {
			Share s = new Share();
			s.location = location;
			s.localPath = path;
			s.noteId = noteId;
			s.sharers = partners;
			entries.put(location, s);
		}
	}
}
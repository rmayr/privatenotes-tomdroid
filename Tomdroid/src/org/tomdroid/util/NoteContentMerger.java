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
package org.tomdroid.util;

import java.util.Arrays;
import java.util.List;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.StringUtills;

public class NoteContentMerger {
	/** regex line split */
	private static final String LINESPLIT = "\\r?\\n";
	
	private String merged;
	
	private String prefInsert = "&gt;";
	private String prefDelete = "&lt;";	
	private String prefDiff = "=";
	
	public NoteContentMerger(String content1, String content2) {
		List<String> c1 = Arrays.asList(content1.split(LINESPLIT));
		List<String> c2 = Arrays.asList(content2.split("\\r?\\n"));
				
		Patch p = DiffUtils.diff(c1, c2);
		List<Delta> diffs = p.getDeltas();
		
		StringBuilder sb = new StringBuilder();
		int pos = 0;
		for (Delta d : diffs) {
			int cpos = d.getOriginal().getPosition();
			for (int i = pos; i < cpos; i++) {
				// full up to the current point:
				sb.append(c1.get(i));
				sb.append("\n");
			}
			pos = cpos + d.getOriginal().getSize();
			//System.out.println(d.getOriginal());
			//System.out.println(d.getRevised());
			String orig = StringUtills.join(d.getOriginal().getLines(), "\n");
			String now = StringUtills.join(d.getRevised().getLines(), "\n");
			int lorig = orig.trim().length();
			int lnow = now.trim().length();
			
			if (lorig < 1 && lnow > 1) {
				// insertion
				sb.append(prefInsert);
				sb.append(now);
				sb.append('\n');
			} else if (lorig > 1 && lnow < 1) {
				// deletion
				sb.append(prefDelete);
				sb.append(orig);
				sb.append('\n');
			} else {
				// diff
				sb.append(prefDiff);
				sb.append(orig);
				sb.append('\n');
				sb.append(prefDiff);
				sb.append(now);
				sb.append('\n');
			}
		}
		
		merged = sb.toString();
	}
	
	public String getMerged() {
		return merged;
	}

}

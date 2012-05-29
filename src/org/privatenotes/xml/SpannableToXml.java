/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2012 Paul Klingelhuber <paul.klingelhuber@students.fh-hagenberg.at>
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
package org.privatenotes.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.privatenotes.Note;
import org.xmlpull.v1.XmlSerializer;

import android.text.Editable;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.Xml;

/**
 * 
 * converts an android editableText to a tomboy-compatible xml format
 * 
 */
public class SpannableToXml {
	
	private static final String TAG = "SpannableToXml";
	
	// -- Tomboy's notes XML tags names -- 
	// Style related
	private final static String BOLD = "bold";
	private final static String ITALIC = "italic";
	private final static String STRIKETHROUGH = "strikethrough";
	private final static String HIGHLIGHT = "highlight";
	private final static String MONOSPACE = "monospace";
	private final static String SMALL = "size:small";
	private final static String LARGE = "size:large";
	private final static String HUGE = "size:huge";
	// Bullet list-related
	public final static String LIST = "list";
	public final static String LIST_ITEM = "list-item";	
	
	private static final String NS = null;
	
	private int currentListDepth = 0;
	
	private String result = ""; 
	
	public SpannableToXml() {
		
	}
	
	public String getResult() {
		return result;
	}
	
	public void convert(Editable text) {
		XmlSerializer ser = Xml.newSerializer();
		StringWriter sw = new StringWriter();
		try {
			ser.setOutput(sw);
			
			doTextContent(ser, text);
			
			ser.flush();
			result = sw.toString();
		} catch (IOException ioe) {
			Log.w(TAG, "xml serialization failed ", ioe);
		}
	}
	
	private void doTextContent(XmlSerializer ser, Editable text) throws IOException {
		int length = text.length();
		Object[] allspans = text.getSpans(0, length, Object.class);
		// we only are interested in types:
		// BulletSpan
		// CharacterStyle
		
		List<SpanInfo> spans = new ArrayList<SpanInfo>();
		for (Object span : allspans) {
			if (span instanceof LeadingMarginSpan || span instanceof CharacterStyle) {
				SpanInfo info = SpanInfo.create(text, span);
				if (info != null) {
					spans.add(info);
				}
			}
		}
		
		Stack<String> currentlyOpenTags = new Stack<String>();
		
		StringBuffer sb = new StringBuffer();
		
		for (int i=0; i<length; i++) {
			List<SpanInfo> startOrEnd = spansFor(spans, i);
			int desiredDepth = getListDepth(startOrEnd, i);
			
			if (startOrEnd.size() != 0) {
				ser.text(sb.toString());
				sb.delete(0, sb.length());
			}
			
			// first check all that are ending:
			for (SpanInfo s : startOrEnd) {
				if (s.end == i) {
					String name = s.spanName;
					if (LIST.equals(name)) {
						// lists are treated in another way (may not always be closed when this is over
						continue;
					}
					if (LIST_ITEM.equals(name)) {
						if (!continuesList(startOrEnd, i)) {
							// also close the list
							closeAllTagsUntil(ser, currentlyOpenTags, name);
							closeAllTagsUntil(ser, currentlyOpenTags, LIST);
							currentListDepth--;
						}
						else {
							closeAllTagsUntil(ser, currentlyOpenTags, name);
						}
					} else {
						// close until the last opened is here
						closeAllTagsUntil(ser, currentlyOpenTags, name);
					}
				}
			}
			
			// now process all that start here
			for (SpanInfo s : startOrEnd) {
				if (s.start == i) {
					String name = s.spanName;
					if (LIST.equals(name)) {
						continue;
					}
					// if its a list-item-tag
					// we might have to open/close list-tags before
					if (LIST_ITEM.equals(name)) {
						if (currentListDepth == desiredDepth) {
							// ignore, everything ok!
						}
						else if (desiredDepth > currentListDepth) {
							for (int x=0; x<(desiredDepth - currentListDepth); x++) {
								ser.startTag(NS, LIST);
								currentlyOpenTags.push(LIST);
								currentListDepth++;
							}
						}
						else if (desiredDepth < currentListDepth) {
							while (desiredDepth < currentListDepth) {
								closeAllTagsUntil(ser, currentlyOpenTags, LIST);
								currentListDepth--;
							}
						}
					}
					// all other tags (not lists:)
					ser.startTag(NS, name);
					currentlyOpenTags.push(name);
				}
			}
			
			sb.append(text.subSequence(i, i+1));
		}
		
		if (sb.length() > 0) {
			ser.text(sb.toString());
		}
		
		while (currentlyOpenTags.size() > 0) {
			ser.endTag(NS, currentlyOpenTags.pop());
		}
		
	}
	
	/**
	 * reads out the list-depth from the LIST entries and also removes them from the list
	 * they must start here (not end)
	 * @param infos
	 * @return the list depth for this point
	 */
	private int getListDepth(List<SpanInfo> infos, int pos) {
		for (int i=0; i<infos.size(); i++) {
			SpanInfo si = infos.get(i);
			if (si.start != pos)
				continue;
			
			int depth = infos.get(i).listDepth; 
			if (depth > 0) {
				// found
				infos.remove(i);
				return depth;
			}
		}
		Log.w(TAG, "This should not happen, there MUST be a list-entry!");
		return 0;
	}
	
	/**
	 * checks if a list item ends here and another one immediately starts
	 * @param infos
	 * @param pos
	 * @return
	 */
	private boolean continuesList(List<SpanInfo> infos, int pos) {
		boolean isListEnd = false;
		for (SpanInfo si : infos) {
			if (si.end == pos && LIST_ITEM.equals(si.spanName)) {
				isListEnd = true;
				break;
			}
		}
		if (!isListEnd)
			return false;
		for (SpanInfo si : infos) {
			if (si.start == pos && LIST_ITEM.equals(si.spanName)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param ser
	 * @param currentlyOpenTags
	 * @param name until we arrive at a tag with this name, will be closed as well, all others will be re-opened
	 */
	private void closeAllTagsUntil(XmlSerializer ser, Stack<String> currentlyOpenTags, String name) throws IOException {
		Stack<String> openAgain = new Stack<String>();
		int openSize = currentlyOpenTags.size();
		for (int j=0; j<openSize; j++) {
			String current = currentlyOpenTags.pop();
			if (current.equals(name)) {
				// this is the one we are looking for
				ser.endTag(NS, name);
				break;
			} else {
				ser.endTag(NS, current);
				openAgain.push(current);
			}
		}
		// now open them again
		while (!openAgain.empty()) {
			String reopen = openAgain.pop();
			ser.startTag(NS, reopen);
			currentlyOpenTags.push(reopen);
		}
	}
	
	public static String getNameForSpan(Object spanObj) {
		if (spanObj instanceof StyleSpan) {
			int style = ((StyleSpan)spanObj).getStyle();
			if (style == android.graphics.Typeface.BOLD)
				return BOLD;
			if (style == android.graphics.Typeface.ITALIC)
				return ITALIC;
			throw new UnsupportedOperationException("invalid style!");
		}
		if (spanObj instanceof StrikethroughSpan) {
			return STRIKETHROUGH;
		}
		if (spanObj instanceof BackgroundColorSpan) {
			return HIGHLIGHT;
		}
		if (spanObj instanceof TypefaceSpan) {
			return MONOSPACE;
		}
		if (spanObj instanceof RelativeSizeSpan) {
			RelativeSizeSpan size = (RelativeSizeSpan)spanObj;
			if (size.getSizeChange() == Note.NOTE_SIZE_SMALL_FACTOR)
				return SMALL;
			if (size.getSizeChange() == Note.NOTE_SIZE_LARGE_FACTOR)
				return LARGE;
			return HUGE;
		}
		if (spanObj instanceof BulletSpan) {
			return LIST_ITEM;
		}
		Log.w(TAG, "unsupported style " + spanObj.getClass().getName());
		return null;
	}
	
	/**
	 * retrieves all spans that start or end at this position
	 * @param pos
	 * @return
	 */
	private List<SpanInfo> spansFor(List<SpanInfo> spans, int pos) {
		List<SpanInfo> active = new ArrayList<SpanInfo>();
		List<SpanInfo> past =new ArrayList<SpanInfo>();
		for (SpanInfo si : spans) {
			if (si.start == pos || si.end == pos) {
				active.add(si);
			}
			else if (si.end < pos) {
				past.add(si);
			}
		}
		for (SpanInfo si : past) {
			spans.remove(si);
		}
		return active;
	}
	
}

class SpanInfo {
	public Object span;
	public int start;
	public int end;
	public String spanName;
	public int listDepth = 0;
	
	private SpanInfo(Editable e, Object span, String name) {
		this.span = span;
		start = e.getSpanStart(span);
		end = e.getSpanEnd(span);
		spanName = name;
	}
	
	public static SpanInfo create(Editable e, Object span) {
		if ((span instanceof LeadingMarginSpan) && (!(span instanceof BulletSpan))) {
			int size = ((LeadingMarginSpan)span).getLeadingMargin(true);
			int depth = size / NoteContentHandler.BULLET_INSET_PER_LEVEL;
			SpanInfo result = new SpanInfo(e, span, SpannableToXml.LIST);
			result.listDepth = depth;
			return result;
		}
		String name = SpannableToXml.getNameForSpan(span);
		if (name == null)
			return null;
		return new SpanInfo(e, span, name);
	}
}

/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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
package org.privatenotes.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlUtils {
	
	/**
	 * Useful to replace the characters forbidden in xml by their escaped counterparts
	 * Ex: &amp; -> &amp;amp;
	 * 
	 * @param input the string to escape
	 * @return the escaped string
	 */
	public static String escape(String input) {
		return input
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("\'", "&apos;");
	}
	
	/**
	 * Useful to replace the escaped characters their unescaped counterparts
	 * Ex: &amp;amp; -> &amp;
	 * 
	 * @param input the string to unescape
	 * @return the unescaped string
	 */
	public static String unescape(String input) {
		return input
			.replace("&amp;", "&")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"")
			.replace("&apos;", "\'");
	}
	
	/**
	 * this method allows you to insert a tag (only attribute-free, like tomboys formatting tags
	 * are currently supported) somewhere while keeping the elements not-overlapping.
	 * e.g. if you want to insert <bold>, </bold>, and a </highlight> is in themiddle, you get this:
	 * <bold>....</bold></highlight><bold>....</bold>, because somewhere in the beginning there has to be
	 * a <highlight> this should keep the xml consistent and keep the parsers from crashing
	 * @param text
	 * @param tagName
	 * @param startPos
	 * @param endPos
	 * @return
	 */
	public static StringBuffer safeInsertTag(StringBuffer text, String tagName, int startPos, int endPos) {
		StringBuffer result =  new StringBuffer();
		result.append(text.subSequence(0, startPos));
		
		List<String> tags = new ArrayList<String>();
		int idx = startPos;
		
		final String startTag = "<" + tagName + ">";
		final String endTag = "</" + tagName + ">";
		
		result.append(startTag);
		
		int lastPos = startPos;
		idx = text.indexOf("<", idx);
		while (idx > 0 && idx < text.length() && idx < endPos) {
			result.append(text.subSequence(lastPos, idx));
			String nextNodeText = text.substring(idx, text.indexOf(">", idx)+1);
			String foundTag = getTagName(nextNodeText);
			if (foundTag.startsWith("/")) {
				// end tag:
				String last = (tags.size() <= 0)?"":tags.get(tags.size() - 1);
				if (last.equals(foundTag.substring(1))) {
					// a tag that has been started has ended now, this is ok
					tags.remove(tags.size() - 1);
					result.append(nextNodeText);
				} else {
					// this is a node that we didn't know about before
					// we have to end our span and start it again afterwards:
					result.append(endTag);
					result.append(nextNodeText);
					result.append(startTag);
				}
			} else {
				// a new tag is started
				tags.add(foundTag);
				result.append(nextNodeText);
			}
			// step over the found tag
			idx += nextNodeText.length();
			
			lastPos = idx;
			idx = text.indexOf("<", idx);
		}
		
		if (idx > endPos) {
			// append until the pos where we want to stop our tag
			result.append(text.subSequence(lastPos, endPos));
			idx = lastPos;
		} else {
			// append all the rest
			result.append(text.subSequence(lastPos, text.length()));
			idx = text.length();
		}
		
		for (String et : tags)
			result.append("</" + et + ">");
		
		result.append(endTag);
		
		for (String et : tags)
			result.append("<" + et + ">");
		
		if (idx < text.length())
			result.append(text.substring(idx, text.length()));
		
		
		return result;
	}
	
	/**
	 * finds a tag name in a given string works for start and end tags
	 * if it is an end-tag the / will be included in the returned name
	 * @param span
	 * @return
	 */
	private static String getTagName(String span) {
		Pattern p = Pattern.compile("<(/?\\w+)\\b");
		Matcher m = p.matcher(span);
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}
	
}

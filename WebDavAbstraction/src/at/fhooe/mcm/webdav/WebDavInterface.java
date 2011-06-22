/*
 * WebDavAbstraction
 * A WebDav abstraction for different libraries
 * 
 * Copyright 2011 Paul Klingelhuber <paul.klingelhuber@students.fh-hagenberg.at>
 * 
 * This file is part of WebDavAbstraction.
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
 * along with WebDavAbstraction.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.fhooe.mcm.webdav;

import it.could.util.http.WebDavClient;
import it.could.util.location.Location;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Vector;

/**
 * A Webdav client based on the it.could.util.http.WebDavClient library.
 * It is a VERY basic library but the only one we could find that runs on Android properly
 * @author Paul Klingelhuber
 *
 */
public class WebDavInterface implements IWebDav {
	private WebDavClient wdc;
	
	/**
	 * {@inheritDoc}
	 */
	WebDavInterface(String url) throws Exception {
		wdc = new WebDavClient(Location.parse(url));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Vector<String> getAllChildren() {
		Vector<String> results = new Vector<String>();
		//Iterator it = null;
		@SuppressWarnings("rawtypes")
		Iterator it = wdc.iterator();
		try {
			while (it.hasNext()) {
				String child = (String)it.next();
				String type = wdc.getContentType(child);
				if (type == null) {
					// type unknown to server
					results.add(child);
				} else if (type.contains("directory")) {
					// directory
					results.add(child + "/");
				} else {
					// all other things (other types known to server)
					results.add(child);	
				}
			}
		} catch (IOException ioe) {
			System.err.println("error while acquiring content type, not good! " + ioe.getMessage());
			ioe.printStackTrace();
		}
		return results;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean download(String _file, File _toLocation) {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = wdc.get(_file);
			out =  new FileOutputStream(_toLocation);
			int b = in.read();
			while (b>=0) {
				out.write(b);
				b = in.read();
			}
			in.close();
			out.close();
			
			return true;
		} catch (Exception e) {
			System.err.println("could not downlaod: " + e.getMessage());
			e.printStackTrace();
			tryClose(in);
			tryClose(out);
			return false;
		}
		
	}
	
	public boolean upload(File _fromLocation, String _toFile) {
		InputStream in = null;
		OutputStream out = null;
		byte[] temp = new byte[1024];
		int count = 0;
		try {
			// first we have to buffer it all up, because our webdav lib forces us to tell
			// it how long the file will be
			// else we get corrupt files with LOTS of null-bytes @ the end
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			in = new FileInputStream(_fromLocation);
			count = in.read(temp);
			while (count >= 0) {
				buffer.write(temp, 0, count);
				count = in.read(temp);
			}
			temp = buffer.toByteArray();
			buffer.close();
			
			out = wdc.put(_toFile, temp.length);
			count = 0;
			out.write(temp, count, temp.length);
			out.flush();
			in.close();
			out.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			tryClose(in);
			tryClose(out);
			return false;
		}
	}
	
	/**
	 * creates the clientInfo object for this implementation
	 * @return ClientInfo object
	 */
	public static ClientInfo getClientInfo() {
		ClientInfo ci = new ClientInfo();
		ci.supportsHttps = false;
		ci.otherInfo = "";
		return ci;
	}
	
	/**
	 * tries to close a closeable
	 * @param closeme
	 */
	private void tryClose(Closeable closeme) {
		if (closeme != null) {
			try {
				closeme.close();
			} catch (IOException e) {
			}
		}
	}
	
}

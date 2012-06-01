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

/**
 * Factory that creates some implementation of IWebDav
 *
 */
public class WebDavFactory {
	
	private WebDavFactory() {}
	
	/**
	 * 
	 * @param url the server-path to construct client for
	 * @return IWebDAV instance
	 */
	public static IWebDav getClient(String url) throws Exception {
		return new WebDavInterface(url);
	}
	
	/**
	 * gets the client-info object for the default client
	 * @return ClientInfo obj with information about that webdav impl
	 */
	public static ClientInfo getClientInfo() {
		return WebDavInterface.getClientInfo();
	}
}

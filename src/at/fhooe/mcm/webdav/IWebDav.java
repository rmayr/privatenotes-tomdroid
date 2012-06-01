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

import java.io.File;
import java.util.Vector;

/**
 * Simple interface for accessing a webdav server via some client
 * 
 * this interface is used to abstract different webdav libraries
 * 
 * @author Paul Klingelhuber
 */
public interface IWebDav {

	/**
	 * gets a list of all files/folders in the current directory
	 * @return
	 */
	public abstract Vector<String> getAllChildren();

	/**
	 * downloads a file
	 * @param _file
	 * @param _toLocation
	 * @return
	 */
	public abstract boolean download(String _file, File _toLocation);

	/**
	 * uploads a file
	 * @param _fromLocation
	 * @param _toFile
	 * @return
	 */
	public abstract boolean upload(File _fromLocation, String _toFile);

}
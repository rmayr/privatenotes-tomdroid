
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
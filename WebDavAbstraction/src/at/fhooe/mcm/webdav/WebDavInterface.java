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

public class WebDavInterface implements IWebDav {
	private WebDavClient wdc;
	
	public WebDavInterface(String url) throws Exception {
		wdc = new WebDavClient(Location.parse(url));
	}
	
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
	
	private void tryClose(Closeable closeme) {
		if (closeme != null) {
			try {
				closeme.close();
			} catch (IOException e) {
			}
		}
	}
	
}

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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.spongycastle.util.encoders.Base64;

import android.util.Log;
import at.fhooe.mcm.tomboyCrypt.Util;

/**
 * A Webdav client based on the Sardine library.
 * It is a VERY basic library
 * @author Paul Klingelhuber
 *
 */
public class WebDavInterface implements IWebDav {
	private String user;
	private String pw;
	private String url;
	
	
	public static void main(String[] args) throws Exception {
		URL x = new URL("http://USER:PASSWORD@privatenotes.dyndns-server.com/webdav2/USER");
		System.out.println(x.getAuthority());
		/*
		WebDavInterface wdi = new WebDavInterface("http://test333:test@privatenotes.dyndns-server.com/webdav2/test333");
		Vector<String> children = wdi.getAllChildren();
		for (String c : children) {
			System.out.println(c);
		}
		wdi.download("manifest.xml", new File("D:/mymanifest.xml"));
		
		wdi.upload(new File("D:/hello.txt"), "supersecretNOT.txt");*/
	}
	
	/**
	 * {@inheritDoc}
	 */
	WebDavInterface(String url) throws Exception {		
		try {
			URL x = new URL(url);
			String userInfo = x.getUserInfo();
			int sepIdx = userInfo.lastIndexOf(':');
			if (sepIdx != -1) {
				user = userInfo.substring(0, sepIdx );
				pw = userInfo.substring(sepIdx + 1);
				
				String basePath = url;
				if (!basePath.endsWith("/")) {
					basePath = basePath + "/";
				}
				
				this.url = "http://" + x.getHost() + "/account/webdav.php";
				
				System.out.println(basePath);
				
			}
			else
			{
				throw new Exception("Server url not correctly configured!"
							+"Needs http://USER:PASSWORD@privatenotes.dyndns-server.com/webdav2/USER");
			}
		} catch (MalformedURLException e) {
			throw new Exception("Server url not correctly configured!"
					+"Needs http://USER:PASSWORD@privatenotes.dyndns-server.com/webdav2/USER");
		}
	}
	
	/**
	 * executes a http post request
	 * 
	 * also adds the user/password to the request params
	 * 
	 * @param values
	 * @return
	 */
	private RestResponse doRequest(List<NameValuePair> values) {
		HttpClient httpclient = new DefaultHttpClient();

		// Prepare a request object
		HttpPost post = new HttpPost(url);

		// Execute the request
		String data = "";
		int returnCode = -1;
		try {
			values.add(new BasicNameValuePair("user", user));
			values.add(new BasicNameValuePair("pw", pw));
			
			HttpEntity entity = new UrlEncodedFormEntity(values);
			post.setEntity(entity);
			HttpResponse response = httpclient.execute(post);
			// Examine the response status
			
			returnCode = response.getStatusLine().getStatusCode();
			data = convertStreamToString(response.getEntity().getContent());
			
			return new RestResponse(returnCode, data); 
			
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			data = e.getClass().getName() + " " + e.getMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			data = e.getClass().getName() + " " + e.getMessage();
		}
		
		return new RestResponse(-1, data);
	}
	
	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return sb.toString();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Vector<String> getAllChildren() {
		Vector<String> results = new Vector<String>();
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("type", "LIST"));
		
        RestResponse response = doRequest(nameValuePairs);
        
        if (response.isOk()) {
        	String lines[] = response.data.split("\\r?\\n");
        	// prevent empty lines
        	for (String line : lines) {
        		if (!"".equals(line.trim())) {
        			results.add(line);
        		}
        	}
        }
        
		return results;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean download(String _file, File _toLocation) {
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("type", "GET"));
        nameValuePairs.add(new BasicNameValuePair("file", _file));
		
        RestResponse response = doRequest(nameValuePairs);
        
        if (response.isOk()) {
        	byte[] fileData = Base64.decode(response.data);
        	OutputStream fout = null;
        	try {
	        	fout = new FileOutputStream(_toLocation);
	        	fout.write(fileData);
	        	fout.close();
        	}
        	catch (Exception e) {
        		Log.e("", e.getMessage(), e);
        		tryClose(fout);
        	}
        }
        return false;
	}
	
	public boolean upload(File _fromLocation, String _toFile) {
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		try {
			InputStream fin = new FileInputStream(_fromLocation);
			
			byte[] fileData = Util.readStreamFully(fin);
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			Base64.encode(fileData, buffer);		
		
	        nameValuePairs.add(new BasicNameValuePair("type", "PUT"));
	        nameValuePairs.add(new BasicNameValuePair("file", _toFile));
	        nameValuePairs.add(new BasicNameValuePair("content", buffer.toString("UTF-8")));
	    } catch (Exception e) {
			Log.w("WebDav", e.getMessage());
			return false;
		}
        
        RestResponse response = doRequest(nameValuePairs);
        
        return response.isOk();
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
	
	public static class RestResponse {
		public int code;
		public String data;
		public RestResponse(int code, String data) {
			this.code = code;
			this.data = data;
		}
		
		public boolean isOk() {
			return code >= 200 && code <300;
		}
	}
	
}

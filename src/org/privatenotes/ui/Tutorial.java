/*
 * PrivateNotes
 * 
 * Copyright 2011 Paul Klingelhuber <paul.klingelhuber@students.fh-hagenberg.at>
 * 
 * This file is part of PrivateNotes.
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
package org.privatenotes.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

import org.spongycastle.util.encoders.Base64;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Tutorial extends Activity
{

	// Logging info
	private static final String TAG = "WebView";

	// UI to data model glue
	private WebView webView;
	
	// HTML page displaying "loading...\nPleaseWait." centered on page
	private static final String loadingText = "<html style=\"height:100%;\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"></head><body><div style=\"width:100%;text-align:center;position:absolute;top:50%;margin-top:-1em;font-size:x-large;font-family:sans-serif;\">Loading...<br/>Please wait.</div></body></html>";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		webView = new WebView(this);
		setContentView(webView);
		
		webView.setWebViewClient(new WebViewClient() {
				@Override
				public boolean shouldOverrideUrlLoading (WebView view, String url) {
					if (url.toLowerCase().contains("isdone")) {
						try {
							Tutorial.this.finish();
							Tutorial.this.finalize();
						} catch (Throwable e) {
							Log.e(TAG, "could not close the activity", e);
						}
						return true;
					}
					return false;
				}
				
				@Override
				public void onPageFinished(WebView view, String url) {
					if (!url.startsWith("http")) {
						Log.d(TAG, "placeholder loaded, load url now");
						webView.loadUrl("http://dl.dropbox.com/u/1526874/PrivateNotes/setup/index.html");
					}
				}
		});
		
		webView.getSettings().setJavaScriptEnabled(true);
		
		// display loading before doing actual loading
		webView.loadData(toBase64(loadingText), "text/html", "base64");
	}
	
	private String toBase64(String data) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			Base64.encode(data.getBytes("UTF-8"), buffer);
			return buffer.toString("UTF-8");
		} catch (IOException e) {
			return null;
		}
	}

}

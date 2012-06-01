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
		});
		
		webView.getSettings().setJavaScriptEnabled(true);
		
		webView.loadUrl("http://dl.dropbox.com/u/1526874/PrivateNotes/setup/index.html");
	}

}

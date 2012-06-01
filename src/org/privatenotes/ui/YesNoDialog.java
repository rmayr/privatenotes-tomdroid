/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2011 Paul Klingelhuber <paul.klingelhuber@students.fh-hagenberg.at>
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
package org.privatenotes.ui;

import org.privatenotes.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

/**
 * easy dialog to let the user decide something
 * 
 */
public class YesNoDialog {
	
	private static String labelText = "Continue?";
	
	private static OnClickListener defaultAction = new OnClickListener() {
		
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
		}
	};

	
	/**
	 * Show a dialog to the user where he can decide something
	 * @param from
	 * @param title
	 * @param text
	 * @param yesBtn
	 * @param noBtn
	 * @param yes may be null (will dismiss)
	 * @param no may be null (will dismiss)
	 * @return
	 */
	public static Dialog createDialog(Activity from, String title, String text, String yesBtn, String noBtn, OnClickListener yes, OnClickListener no) {
		if (text == null)
			text = labelText;
		if (yes == null)
			yes = defaultAction;
		if (no == null)
			no = defaultAction;
		if (yesBtn == null)
			yesBtn = from.getString(R.string.btnOk);
		if (noBtn == null)
			noBtn = from.getString(R.string.btnCancel);
		
		Dialog d = new AlertDialog.Builder(from).setMessage(text).setTitle(title)
				.setPositiveButton(yesBtn, yes)
				.setNegativeButton(noBtn, no)
				.setIcon(R.drawable.icon).create();
		
		return d;
	}
}

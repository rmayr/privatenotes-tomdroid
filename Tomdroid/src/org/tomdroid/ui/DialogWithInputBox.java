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
package org.tomdroid.ui;

import org.tomdroid.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;

/**
 * easy input-dialog (extend this class to use it)
 * 
 * @author Paul Klingelhuber
 */
public class DialogWithInputBox {
	
	protected String labelText = "Please enter some text:";
	protected String startInputText = "";
	
	public static final String INTENT_DIALOG_TEXT = "org.tomdroid.dialog.text";
	public static final String INTENT_DIALOG_ENTEREDTEXT = "org.tomdroid.dialog.enteredtext";
	
	private AlertDialog.Builder alert;
	private EditText input;
	
	
	/** shows the input dialog */
	public void showDialog(Activity from, String text, String defaultInput) {
		if (text != null)
			labelText = text;
		if (defaultInput != null)
			startInputText = defaultInput;
		alert = new AlertDialog.Builder(from);
		alert.setMessage(getLabelText());
		input = new EditText(from);
		input.setText(getInputText());
		
		alert.setView(input);
		alert.setPositiveButton(from.getString(R.string.btnOk), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString().trim();
				onPositiveReaction(value);
			}
		});

		alert.setNegativeButton(from.getString(R.string.btnCancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
						onNegativeReaction();
					}
				});
		alert.show();
	}
	
	/**
	 * overwrite this to set label text
	 * @return
	 */
	protected String getLabelText() {
		return labelText;
	}
	
	/**
	 * overwrite this to set input text
	 * @return
	 */
	protected String getInputText() {
		return startInputText;
	}

	/**
	 * executed when user clicks ok
	 * @param enteredText
	 */
	protected void onPositiveReaction(String enteredText) {
		// template method, override this
	}
	
	/**
	 * executed when user clicks cancel
	 */
	protected void onNegativeReaction() {
		// template method, override this
	}

}

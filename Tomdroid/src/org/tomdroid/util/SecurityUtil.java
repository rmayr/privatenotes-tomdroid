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
package org.tomdroid.util;

import org.tomdroid.ui.Tomdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * a class for handling (temporary) password storage, the password-input dialog
 * and more.
 * 
 * @author Paul Klingelhuber, some code based on Patrick Gabors work
 *
 */
public class SecurityUtil {
	private static final String TAG = "SecurityUtil";
	
	private static final SecurityUtil INSTANCE = new SecurityUtil();
	private SecurityUtil() {
	}
	
	public static SecurityUtil getInstance() {
		return INSTANCE;
	}
	
	// Password storage fields
	private static final String PASSWORD_STORAGE = "password_storage";
	private static final String PASSWORD_FIELD = "password_field";
	
	private static String transientPassword = "";
	
	/**
	 * load password from storage
	 * 
	 * @return
	 */
	public String getPassword(Context context)
	{
		boolean savePw = Preferences.getBoolean(Preferences.Key.SAVE_PASSWORD);
		if (savePw) {
			SharedPreferences profileStored = context.getSharedPreferences(PASSWORD_STORAGE, 0);
	
			try
			{
				return profileStored.getString(PASSWORD_FIELD, "");
	
			} catch (Exception ex)
			{
				ex.printStackTrace();
				return "";
			}
		} else {
			return transientPassword;
		}
	}
	
	public String getPassword() {
		return getPassword(Tomdroid.getInstance().getApplicationContext());
	}
	
	/**
	 * sets a new password
	 * 
	 * depending on the status of the SAVE_PASSWORD setting, it is either only saved to the
	 * ram or to the preferences. Either way, the other data is ALWAYS removed
	 * 
	 * @param newPassword
	 */
	public void setPassword(Context context, String newPassword) {
		boolean savePw = Preferences.getBoolean(Preferences.Key.SAVE_PASSWORD);
		
		String persistedPw = "";
		String tempPw = "";
		if (savePw) {
			persistedPw = newPassword;
		} else {
			Log.d(TAG, "removing persisted pw");
			tempPw = newPassword;
		}
		
		// peristent
		SharedPreferences profileStored = context.getSharedPreferences(PASSWORD_STORAGE, 0);
		SharedPreferences.Editor editor = profileStored.edit();
		try
		{
			editor.putString(PASSWORD_FIELD, persistedPw);
			editor.commit();
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		// temporary
		transientPassword = tempPw;
	}
	
	/**
	 * @see setPassword(Context context, String newPassword)
	 */
	public void setPassword(String newPassword) {
		setPassword(Tomdroid.getInstance().getApplicationContext(), newPassword);
	}

}

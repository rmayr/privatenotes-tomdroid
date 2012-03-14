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
import at.fhooe.mcm.tomboyCrypt.CryptoSchemeProvider;
import at.fhooe.mcm.tomboyCrypt.PgpCryptoSchemeBc;

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
	private static final String PASSWORD_GPG_FIELD = "password_gpg_field";
	
	private static byte[] transientPassword = "".getBytes();
	private static byte[] transientGpgPassword = "".getBytes();
	
	/**
	 * load password from storage
	 * 
	 * @return
	 */
	public byte[] getPassword(Context context)
	{
		boolean savePw = Preferences.getBoolean(Preferences.Key.SAVE_PASSWORD);
		if (savePw) {
			SharedPreferences profileStored = context.getSharedPreferences(PASSWORD_STORAGE, 0);
	
			try
			{
				return profileStored.getString(PASSWORD_FIELD, "").getBytes();
	
			} catch (Exception ex)
			{
				ex.printStackTrace();
				return "".getBytes();
			}
		} else {
			return transientPassword;
		}
	}
	
	public byte[] getPassword() {
		return getPassword(Tomdroid.getInstance().getApplicationContext());
	}
	
	public byte[] getGpgPassword() {
		Context context = Tomdroid.getInstance().getApplicationContext();
		boolean savePw = Preferences.getBoolean(Preferences.Key.SAVE_PASSWORD);
		if (savePw) {
			SharedPreferences profileStored = context.getSharedPreferences(PASSWORD_STORAGE, 0);
	
			try
			{
				return profileStored.getString(PASSWORD_GPG_FIELD, "").getBytes();
	
			} catch (Exception ex)
			{
				ex.printStackTrace();
				return "".getBytes();
			}
		} else {
			return transientGpgPassword;
		}
	}
	
	/**
	 * check if we need (user has to enter) the gpg password
	 * @return true if user has to tell us (pretty please)
	 */
	public boolean needsGpgPassword() {
		Object cs = CryptoSchemeProvider.getConfiguredCryptoScheme();
		if (cs instanceof PgpCryptoSchemeBc) {
			boolean savePw = Preferences.getBoolean(Preferences.Key.SAVE_PASSWORD);
			return !savePw || (savePw && new String(getGpgPassword()).equals(""));
		}
		return false;
	}
	
	/**
	 * sets a new password
	 * 
	 * depending on the status of the SAVE_PASSWORD setting, it is either only saved to the
	 * ram or to the preferences. Either way, the other data is ALWAYS removed
	 * 
	 * @param newPassword
	 * @param normalPassword if true the normal (symm. enc. password) is stored, if false the gpg-password is stored
	 */
	public void setPassword(Context context, byte[] newPassword, boolean normalPassword) {
		final Preferences.Key saveKey = normalPassword?Preferences.Key.SAVE_PASSWORD : Preferences.Key.SAVE_PASSWORD;
		final String fieldKey = normalPassword?PASSWORD_FIELD : PASSWORD_GPG_FIELD;
		boolean savePw = Preferences.getBoolean(saveKey);
		
		byte[] persistedPw = "".getBytes();
		byte[] tempPw = "".getBytes();
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
			editor.putString(fieldKey, new String(persistedPw));
			editor.commit();
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		// temporary
		overwrite(normalPassword?transientPassword:transientGpgPassword);
		if (normalPassword) {
			transientPassword = tempPw;
		} else {
			transientGpgPassword = tempPw;
		}
	}
	
	/**
	 * @see setPassword(Context context, String newPassword)
	 */
	public void setPassword(byte[] newPassword) {
		setPassword(Tomdroid.getInstance().getApplicationContext(), newPassword, true);
	}
	
	public void setGpgPassword(byte[] newPassword) {
		setPassword(Tomdroid.getInstance().getApplicationContext(), newPassword, false);
	}
	
	public static final void overwrite(byte[] b) {
		if (b != null) {
			for (int i=0; i<b.length; i++) {
				b[i] = 0;
			}
		}
	}

}

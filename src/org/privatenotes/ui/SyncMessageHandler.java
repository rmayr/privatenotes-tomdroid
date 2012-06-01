/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2010, Rodja Trappe <mail@rodja.net>
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

import org.privatenotes.sync.SyncManager;
import org.privatenotes.sync.SyncMethod;
import org.privatenotes.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;
import at.fhooe.mcm.tomboyCrypt.CryptoScheme;
import at.fhooe.mcm.tomboyCrypt.CryptoSchemeProvider;
import at.fhooe.mcm.tomboyCrypt.PgpCryptoScheme;

public class SyncMessageHandler extends Handler {

	private static String TAG = "SycnMessageHandler";
	private Activity activity;

	// State variables
	private boolean parsingErrorShown = false;
	
	public SyncMessageHandler(Activity activity) {
		this.activity = activity;
		PgpCryptoScheme.setActivity(activity);
	}

	@Override
	public void handleMessage(Message msg) {

		switch (msg.what) {
			case SyncMethod.PARSING_COMPLETE:
				// TODO put string in a translatable bundle
				Toast.makeText(
						activity,
						"Synchronization with "
								+ SyncManager.getInstance().getCurrentSyncMethod().getDescription()
								+ " is complete.", Toast.LENGTH_SHORT).show();
				break;

			case SyncMethod.PARSING_NO_NOTES:
				// TODO put string in a translatable bundle
				Toast.makeText(
						activity,
						"No notes found on "
								+ SyncManager.getInstance().getCurrentSyncMethod().getDescription()
								+ ".", Toast.LENGTH_SHORT).show();
				break;

			case SyncMethod.PARSING_FAILED:
				if (Tomdroid.LOGGING_ENABLED)
					Log.w(TAG, "handler called with a parsing failed message");

				// if we already shown a parsing error in this pass, we
				// won't show it again
				if (!parsingErrorShown) {
					parsingErrorShown = true;

					// TODO put error string in a translatable resource
					new AlertDialog.Builder(activity).setMessage(
							"There was an error trying to parse your note collection. If "
									+ "you are able to replicate the problem, please contact us!")
							.setTitle("Error").setNeutralButton("Ok", new OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							}).show();
				}
				break;
			case SyncMethod.ENCRYPTION_ERROR:
				if (Tomdroid.LOGGING_ENABLED)
					Log.w(TAG, "invalid password for sync");

				// if we already shown a parsing error in this pass, we
				// won't show it again

				// TODO put error string in a translatable resource
				new AlertDialog.Builder(activity).setMessage(activity.getString(R.string.syncPwError))
						.setTitle(activity.getString(R.string.error)).setNeutralButton(activity.getString(R.string.btnOk), new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).show();
					
				break;
			case SyncMethod.NO_INTERNET:
				// TODO put string in a translatable bundle
				Toast.makeText(activity, "You are not connected to the internet.",
						Toast.LENGTH_SHORT).show();
				break;

			case SyncMethod.SYNC_PROGRESS:
				handleSyncProgress(msg);
				break;

			default:
				if (Tomdroid.LOGGING_ENABLED)
					Log.i(TAG, "handler called with an unknown message");
				break;

		}
	}

	private void handleSyncProgress(Message msg) {
		ImageView syncIcon = (ImageView) activity.findViewById(R.id.syncIcon);

		RotateAnimation rotation = new RotateAnimation(180 * msg.arg2 / 100f,
				180 * msg.arg1 / 100f, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		rotation.setDuration(700);
		rotation.setFillAfter(true);
		if (syncIcon != null)
			syncIcon.startAnimation(rotation);
		else
			Log.w(TAG, "could not find sync icon!");

		if (msg.arg1 == 0) {
			onSynchronizationStarted();
		} else if (msg.arg1 == 100) {
			onSynchronizationDone();
		}
	}

	private void onSynchronizationDone() {
		ImageView syncButton = (ImageView) activity.findViewById(R.id.sync);
		ImageView syncIcon = (ImageView) activity.findViewById(R.id.syncIcon);

		if (syncButton != null && syncIcon != null) {
			syncButton.setClickable(true);
			syncIcon.getDrawable().setAlpha(Actionbar.DEFAULT_ICON_ALPHA);
		} else {
			Log.w(TAG, "could not find sync btn or icon!");
		}

		View dot = activity.findViewById(R.id.sync_dot);
		dot.setVisibility(View.INVISIBLE);
		if (dot.getAnimation() != null) {
			dot.getAnimation().setRepeatCount(0);
		} else {
			Log.w(TAG, "no animation for dot?");
		}
	}

	private void onSynchronizationStarted() {
		ImageView syncButton = (ImageView) activity.findViewById(R.id.sync);
		ImageView syncIcon = (ImageView) activity.findViewById(R.id.syncIcon);

		syncButton.setClickable(false);
		syncIcon.getDrawable().setAlpha(40);
		
		Animation pulse = AnimationUtils.loadAnimation(activity, R.anim.pulse);
		View dot = activity.findViewById(R.id.sync_dot);
		dot.setVisibility(View.VISIBLE);
		dot.startAnimation(pulse);
	}
	
	/**
	 * some sync parts may need activity results, so they must be
	 * forewarded to here
	 * @param from activity form where it cam
	 * @param requestCode 
	 * @param resultCode
	 * @param data
	 */
	public void onActivityResult(Activity from, int requestCode, int resultCode,
            android.content.Intent data) {
		CryptoScheme cs = CryptoSchemeProvider.getConfiguredCryptoScheme();
		if (cs instanceof PgpCryptoScheme) {
			((PgpCryptoScheme) cs).onActivityResult(from, requestCode, resultCode, data);
		} else {
			Log.w(TAG, "no gpg crypto defined!");
		}
	}

}

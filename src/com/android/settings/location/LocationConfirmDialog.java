/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.location;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;

import com.android.settings.R;

import static com.android.settings.location.LocationSettingsBase.KEY_NEVER_ASK;
import static com.android.settings.location.LocationSettingsBase.NEW_MODE_KEY;
import static com.android.settings.location.LocationSettingsBase.PREFERENCE_FILE_NAME;
import static com.android.settings.location.LocationSettingsBase.updateLocationMode;

/**
 * Confirmation dialog for location settings change.
 */
public class LocationConfirmDialog extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dismissNonSecureKeyguard();
        showDialog();
    }

    private void showDialog() {
        final FragmentManager fm = getFragmentManager();
        final String tag = LocationConfirmDialogFragment.TAG;
        if (fm.findFragmentByTag(tag) == null) {
            LocationConfirmDialogFragment fragment =
                LocationConfirmDialogFragment.newInstance(getIntent().getExtras());
            fragment.show(fm, tag);
        }
    }

    private void dismissNonSecureKeyguard() {
        final KeyguardManager km = getSystemService(KeyguardManager.class);
        if (!km.isKeyguardSecure()) {
            km.requestDismissKeyguard(this, null);
        }
    }

    public static class LocationConfirmDialogFragment extends DialogFragment
            implements DialogInterface.OnClickListener {

        public static final String TAG = "LocationConfirmDialogFragment";

        private CheckBox mDontAskAgain;

        public static LocationConfirmDialogFragment newInstance(Bundle args) {
            LocationConfirmDialogFragment fragment = new LocationConfirmDialogFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View view = LayoutInflater.from(getContext())
                    .inflate(R.layout.location_confirm_dialog, null);
            mDontAskAgain = view.findViewById(R.id.do_not_ask_checkbox);
            mDontAskAgain.setChecked(false);

            Dialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.location_setting_warning_title)
                    .setPositiveButton(R.string.location_setting_agree, this)
                    .setNegativeButton(R.string.location_setting_disagree, null)
                    .setView(view)
                    .create();
            dialog.getWindow().setType(LayoutParams.TYPE_STATUS_BAR_PANEL);
            return dialog;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                final int currentMode = Secure.getInt(getContext().getContentResolver(),
                        Secure.LOCATION_MODE, Secure.LOCATION_MODE_OFF);
                final int newMode = getArguments().getInt(NEW_MODE_KEY);
                updateLocationMode(getContext(), currentMode, newMode);

                if (mDontAskAgain.isChecked()) {
                    SharedPreferences.Editor editor = getContext().getSharedPreferences(
                            PREFERENCE_FILE_NAME, Context.MODE_PRIVATE).edit();
                    editor.putBoolean(KEY_NEVER_ASK, true);
                    editor.apply();
                }
            }
            dialog.dismiss();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (isAdded()) {
                getActivity().finish();
            }
        }
    }
}

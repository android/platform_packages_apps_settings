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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.android.settings.R;

/**
 *
 * Dialog for location confirmation.
 *
 */
public class LocationConfirmationDialog extends Activity {

    private static final String PREFERENCE_FILE_NAME = "location_settings";
    private static final String KEY_SHOW_LOCATION_DIALOG = "show_location_dialog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        // Whether the dialog should be displayed, check before creating this activity
        // with using shouldShowDialog()
        LocationConfirmationDialogFragment.showDialog(getFragmentManager(),
                LocationConfirmationDialogFragment.TAG_LOCATION_CONFIRMATION_DIALOG, intent);
    }

    public static boolean hasOptionChecked(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        return !sharedPreferences.getBoolean(KEY_SHOW_LOCATION_DIALOG, true);
    }

    public static boolean shouldShowDialog(Context context, int newMode) {
        if (!context.getResources().getBoolean(R.bool.config_showLocationDialog)
                || hasOptionChecked(context)) {
            return false;
        }
        int currentMode = Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);

        return (currentMode == Settings.Secure.LOCATION_MODE_OFF && currentMode != newMode);
    }

    public static class LocationConfirmationDialogFragment extends DialogFragment {
        public static final String TAG_LOCATION_CONFIRMATION_DIALOG =
                "LocationConfirmationDialogFragment";

        private CheckBox mCheckbox = null;
        private int mNewMode;

        public LocationConfirmationDialogFragment() {
        }

        public LocationConfirmationDialogFragment(Intent intent) {
            if (intent != null && intent.hasExtra(LocationReceiver.INTENT_EXTRA_NEW_MODE)) {
                mNewMode = intent.getIntExtra(LocationReceiver.INTENT_EXTRA_NEW_MODE,
                        Settings.Secure.LOCATION_MODE_OFF);
            }
        }

        public static void showDialog(FragmentManager fm, String tag, Intent intent) {
            if (fm.findFragmentByTag(tag) == null) {
                final LocationConfirmationDialogFragment dialog =
                        new LocationConfirmationDialogFragment(intent);
                dialog.show(fm, tag);
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState != null &&
                    savedInstanceState.containsKey(LocationReceiver.INTENT_EXTRA_NEW_MODE)) {
                mNewMode = savedInstanceState.getInt(LocationReceiver.INTENT_EXTRA_NEW_MODE);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            outState.putInt(LocationReceiver.INTENT_EXTRA_NEW_MODE, mNewMode);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();

            View dialogView = LayoutInflater.from(activity).inflate(
                    R.layout.location_confirmation_dialog, null);

            mCheckbox = (CheckBox) dialogView.findViewById(R.id.location_confirmation_checkbox);
            mCheckbox.setChecked(false);
            if (!getActivity().getResources().getBoolean(
                    R.bool.config_showCheckBoxOnLocationDialog)) {
                mCheckbox.setVisibility(View.GONE);
            }

            return new AlertDialog.Builder(activity)
                    .setTitle(R.string.settings_attention_title)
                    .setPositiveButton(R.string.settings_agree_label,
                            new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            // Update Location Mode
                            Settings.Secure.putInt(
                                    getActivity().getApplicationContext().getContentResolver(),
                                    Settings.Secure.LOCATION_MODE, mNewMode);

                            if (mCheckbox.isChecked()) {
                                SharedPreferences.Editor editor =
                                        getActivity().getSharedPreferences(
                                                PREFERENCE_FILE_NAME, Context.MODE_PRIVATE).edit();
                                editor.putBoolean(KEY_SHOW_LOCATION_DIALOG, false);
                                editor.apply();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.settings_disagree_label, null)
                    .setView(dialogView).create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            final Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.finish();
            }
        }
    }
}

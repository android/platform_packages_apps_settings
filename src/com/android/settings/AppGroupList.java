/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationGroup;
import android.app.ProfileManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.widget.EditText;

public class AppGroupList extends PreferenceActivity {

    private static final String TAG = "AppGroupSettings";

    private ProfileManager mProfileManager;

    private static final int DIALOG_NAME = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.appgroup_list);
        getListView().setItemsCanFocus(true);

        setTitle(R.string.profile_appgroups_title);

        mProfileManager = (ProfileManager) this.getSystemService(PROFILE_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillList();
    }

    private void addNewAppGroup() {
        showDialog(DIALOG_NAME);
    }

    Preference mAddPreference;

    private void fillList() {

        mAddPreference = (PreferenceScreen) findPreference("profile_new_appgroup");

        PreferenceGroup appgroupList = (PreferenceGroup) findPreference("profile_appgroup_list_title");
        appgroupList.removeAll();

        for (NotificationGroup group : mProfileManager.getNotificationGroups()) {

            PreferenceScreen pref = new PreferenceScreen(this, null);

            pref.setKey(group.getName());
            pref.setTitle(group.getName());
            pref.setPersistent(false);

            appgroupList.addPreference(pref);

        }

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Dialog dialog;
        switch (id) {
            case DIALOG_NAME:
                final EditText entry = new EditText(this);
                entry.setPadding(10, 10, 10, 10);
                builder.setMessage(R.string.profile_appgroup_name_prompt);
                builder.setView(entry);
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                NotificationGroup newGroup = new NotificationGroup(entry.getText()
                                        .toString());
                                mProfileManager.addNotificationGroup(newGroup);
                                fillList();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                dialog = builder.create();
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PreferenceScreen) {
            if (mAddPreference == preference) {
                addNewAppGroup();
            } else {
                NotificationGroup group = mProfileManager.getNotificationGroup(preference
                        .getTitle().toString());

                editGroup(group);
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void editGroup(NotificationGroup group) {
        Intent intent = new Intent(this, AppGroupConfig.class);
        intent.putExtra("NotificationGroup", group);
        startActivity(intent);
    }

}

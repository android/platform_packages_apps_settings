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
import android.app.Profile;
import android.app.ProfileGroup;
import android.app.ProfileManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

public class ProfileConfig extends PreferenceActivity implements OnPreferenceChangeListener {

    static final String TAG = "ProfileConfig";

    private static final int DELETE_CONFIRM = 0;

    private ProfileManager mProfileManager;

    private Profile mProfile;

    private EditTextPreference mNamePreference;

    PreferenceScreen mDeletePreference;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.profile_config);
        getListView().setItemsCanFocus(true);

        mProfileManager = (ProfileManager) this.getSystemService(PROFILE_SERVICE);

        Intent input = getIntent();
        Bundle extras = input.getExtras();
        if (extras != null) {
            mProfile = extras.getParcelable("Profile");
        }

        if (mProfile == null) {
            mProfile = new Profile("<new profile>");
            mProfileManager.addProfile(mProfile);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        mProfile = mProfileManager.getProfile(mProfile.getName());

        fillList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save profile here
        if(mProfile != null){
            mProfileManager.addProfile(mProfile);
        }
    }

    private void fillList() {

        mDeletePreference = (PreferenceScreen) findPreference("profile_delete");

        mNamePreference = (EditTextPreference) findPreference("profile_name");

        mNamePreference.setText(mProfile.getName());
        mNamePreference.setSummary(mProfile.getName());
        mNamePreference.setOnPreferenceChangeListener(this);

        PreferenceGroup groupList = (PreferenceGroup) findPreference("profile_appgroups");
        groupList.removeAll();

        for (ProfileGroup profileGroup : mProfile.getProfileGroups()) {

            PreferenceScreen pref = new PreferenceScreen(this, null);

            pref.setKey(profileGroup.getName());
            pref.setTitle(profileGroup.getName());
            // pref.setSummary(R.string.profile_summary);
            pref.setPersistent(false);
            // pref.setSelectable(true);

            groupList.addPreference(pref);

        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Check name isn't alread in use.
        String value = (String) newValue;
        if (mProfileManager.getProfile(value) != null) {
            // Rollback the change.
            return false;
        }
        if (preference == mNamePreference) {
            boolean active = mProfile.getName()
                    .equals(mProfileManager.getActiveProfile().getName());
            mProfileManager.removeProfile(mProfile);
            mProfile.setName(value);
            preference.setSummary(value);
            mProfileManager.addProfile(mProfile);
            if (active) {
                mProfileManager.setActiveProfile(mProfile.getName());
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PreferenceScreen) {
            if (preference == mDeletePreference) {
                deleteProfile();
            } else {
                ProfileGroup profGroup = mProfile.getProfileGroup(preference.getTitle().toString());

                Intent intent = new Intent(this, ProfileGroupConfig.class);
                intent.putExtra("ProfileGroup", profGroup.getName());
                intent.putExtra("Profile", mProfile);
                startActivity(intent);
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    private void deleteProfile() {
        showDialog(DELETE_CONFIRM);
    }
    
    private void doDelete(){
        mProfileManager.removeProfile(mProfile);
        mProfile = null;
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Dialog dialog;
        switch (id) {
            case DELETE_CONFIRM:
                builder.setMessage(R.string.profile_delete_confirm);
                builder.setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doDelete();
                            }
                        });
                builder.setNegativeButton(android.R.string.no,
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


}

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

import android.app.Profile;
import android.app.ProfileManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.util.Log;

public class ProfileList extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ProfileSettings";

    public static final String EXTRA_POSITION = "position";

    public static final String RESTORE_CARRIERS_URI = "content://telephony/carriers/restore";

    public static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";


    private String mSelectedKey;

    private ProfileManager mProfileManager;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.profile_list);
        getListView().setItemsCanFocus(true);

        mProfileManager = (ProfileManager) this.getSystemService(PROFILE_SERVICE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        fillList();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void fillList() {

        PreferenceGroup profileList = (PreferenceGroup) findPreference("profile_title");
        profileList.removeAll();


        mSelectedKey = mProfileManager.getActiveProfile().getName();
        
        for(Profile profile : mProfileManager.getProfiles()){

            ProfilePreference pref = new ProfilePreference(this);

            pref.setKey(profile.getName());
            pref.setTitle(profile.getName());
            pref.setSummary(R.string.profile_summary);
            pref.setPersistent(false);
            pref.setOnPreferenceChangeListener(this);
            pref.setProfile(profile);

            pref.setSelectable(true);
            if ((mSelectedKey != null) && mSelectedKey.equals(profile.getName())) {
                pref.setChecked();
            }
            profileList.addPreference(pref);
        }

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference + ", newValue - " + newValue
                + ", newValue type - " + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedProfile((String) newValue);
        }

        return true;
    }

    private void setSelectedProfile(String key) {
        mSelectedKey = key;
        mProfileManager.setActiveProfile(key);
    }

}

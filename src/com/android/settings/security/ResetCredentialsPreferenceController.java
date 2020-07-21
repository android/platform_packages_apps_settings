/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.security;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class ResetCredentialsPreferenceController extends RestrictedEncryptionPreferenceController
        implements LifecycleObserver, OnResume {

    private static final String KEY_RESET_CREDENTIALS = "credentials_reset";

    private RestrictedPreference mPreference;
    private int mUserId;

    public ResetCredentialsPreferenceController(Context context, int userId, Lifecycle lifecycle) {
        super(context, UserManager.DISALLOW_CONFIG_CREDENTIALS);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mUserId = userId;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_RESET_CREDENTIALS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference.getKey() == mPreference.getKey()) {
            Intent deleteIntent = new Intent(CredentialStorage.ACTION_RESET);
            deleteIntent.setPackage("com.android.settings");
            mContext.startActivityAsUser(deleteIntent, UserHandle.of(mUserId));
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        mPreference.checkRestrictionAndSetDisabled(UserManager.DISALLOW_CONFIG_CREDENTIALS,
                mUserId);
    }

}

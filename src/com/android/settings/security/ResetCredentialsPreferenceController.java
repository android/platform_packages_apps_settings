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
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.KeyStore;

import androidx.preference.PreferenceScreen;

import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class ResetCredentialsPreferenceController extends RestrictedEncryptionPreferenceController
        implements LifecycleObserver, OnResume {

    private static final String KEY_RESET_CREDENTIALS = "credentials_reset";

    private final KeyStore mKeyStore;
    private final UserManager mUm;
    private RestrictedPreference mPreference;
    private final int mType;
    private int mUserId;

    public ResetCredentialsPreferenceController(Context context,
            @ProfileSelectFragment.ProfileType int type, Lifecycle lifecycle) {
        super(context, UserManager.DISALLOW_CONFIG_CREDENTIALS);
        mKeyStore = KeyStore.getInstance();
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mType = type;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mUserId = UserHandle.myUserId();

        if (mType == ProfileSelectFragment.WORK) {
            for (UserInfo user : mUm.getProfiles(UserHandle.myUserId())) {
                if (user.isManagedProfile()) mUserId = user.id;
            }
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_RESET_CREDENTIALS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setOnPreferenceClickListener(
                preference -> {
                    Intent deleteIntent = new Intent(CredentialStorage.ACTION_RESET);
                    deleteIntent.setPackage("com.android.settings");
                    mContext.startActivityAsUser(deleteIntent, UserHandle.of(mUserId));
                    return true;
                });
    }

    @Override
    public void onResume() {
        mPreference.checkRestrictionAndSetDisabled(UserManager.DISALLOW_CONFIG_CREDENTIALS,
                mUserId);
    }

}

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

    private static final String KEY_RESET_CREDENTIALS_PERSONAL = "credentials_reset_personal";
    private static final String KEY_RESET_CREDENTIALS_WORK = "credentials_reset_work";


    private final KeyStore mKeyStore;

    private RestrictedPreference mPreference;
    private int mType;

    public ResetCredentialsPreferenceController(Context context,
            @ProfileSelectFragment.ProfileType int type, Lifecycle lifecycle) {
        super(context, UserManager.DISALLOW_CONFIG_CREDENTIALS);
        mKeyStore = KeyStore.getInstance();
        mType = type;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        if(mType == ProfileSelectFragment.WORK) return KEY_RESET_CREDENTIALS_WORK;
        else return KEY_RESET_CREDENTIALS_PERSONAL;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onResume() {
        if (mPreference != null && !mPreference.isDisabledByAdmin()) {
            mPreference.setEnabled(!mKeyStore.isEmpty());
        }
    }
}

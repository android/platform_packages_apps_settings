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
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.RestrictedPreference;

/**
 * Controller implemented in the Certificates Dashboard fragments in order to manage the
 * installation preference.
 */
public class InstallCertificatePreferenceController
        extends RestrictedEncryptionPreferenceController {

    private static final String KEY_INSTALL_CERTIFICATE = "install_certificate";

    RestrictedPreference mPreference;
    int mUserId;
    Context mContext;

    public InstallCertificatePreferenceController(Context context, int userId) {
        super(context, UserManager.DISALLOW_CONFIG_CREDENTIALS);
        mContext = context;
        mUserId = userId;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_INSTALL_CERTIFICATE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setOnPreferenceClickListener(
                preference -> {
                    new SubSettingLauncher(mContext)
                            .setDestination(InstallCertificateFromStorage.class.getName())
                            .setSourceMetricsCategory(0)
                            .setArguments(null)
                            .setUserHandle(UserHandle.of(mUserId))
                            .launch();
                    return true;
                });
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference.getKey() == mPreference.getKey()) {
            new SubSettingLauncher(mContext)
                    .setDestination(InstallCertificateFromStorage.class.getName())
                    .setSourceMetricsCategory(0)
                    .setArguments(null)
                    .setUserHandle(UserHandle.of(mUserId))
                    .launch();
            return true;
        }
        return false;
    }
}

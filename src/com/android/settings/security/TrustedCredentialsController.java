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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Controller implemented in the Certificates Dashboard fragments in order to manage the
 * installation preference.
 */
public class TrustedCredentialsController extends AbstractPreferenceController {

    private static final String KEY_TRUSTED_CREDENTIALS = "trusted_credentials";
    // String to retrieve user id from Bundle arguments in Trusted Credentials
    public static final String USER_ID = "user_id";

    Preference mPreference;
    int mUserId;
    Context mContext;

    public TrustedCredentialsController(Context context, int userId) {
        super(context);
        mContext = context;
        mUserId = userId;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TRUSTED_CREDENTIALS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference.getKey() == mPreference.getKey()) {
            Bundle args = new Bundle();
            args.putInt(USER_ID, mUserId);
            new SubSettingLauncher(mContext)
                    .setDestination(TrustedCredentialsSettings.class.getName())
                    .setSourceMetricsCategory(SettingsEnums.TRUSTED_CREDENTIALS)
                    .setArguments(args)
                    .launch();
            return true;
        }
        return false;
    }


    @Override
    public boolean isAvailable() {
        return true;
    }
}

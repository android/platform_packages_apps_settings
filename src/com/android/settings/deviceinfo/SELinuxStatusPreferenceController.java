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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.SELinux;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceController;

public class SELinuxStatusPreferenceController extends PreferenceController {

    private static final String PROPERTY_SELINUX_STATUS = "ro.build.selinux";
    private static final String KEY_SELINUX_STATUS = "selinux_status";

    public SELinuxStatusPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(SystemProperties.get(PROPERTY_SELINUX_STATUS));
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SELINUX_STATUS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY_SELINUX_STATUS);
        if (pref == null) {
            return;
        }
        if (!SELinux.isSELinuxEnabled()) {
            String status = mContext.getResources().getString(R.string.selinux_status_disabled);
            pref.setSummary(status);
        } else if (!SELinux.isSELinuxEnforced()) {
            String status = mContext.getResources().getString(R.string.selinux_status_permissive);
            pref.setSummary(status);
        }
    }
}


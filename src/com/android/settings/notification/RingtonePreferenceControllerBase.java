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

package com.android.settings.notification;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceController;

public abstract class RingtonePreferenceControllerBase extends PreferenceController {

    public RingtonePreferenceControllerBase(Context context) {
        super(context);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(mContext, getRingtoneType());
        final CharSequence summary = Ringtone.getTitle(
            mContext, ringtoneUri, false /* followSettingsUri */, true /* allowRemote */);
        if (summary != null) {
            preference.setSummary(summary);
        }
    }

    public abstract int getRingtoneType();

}

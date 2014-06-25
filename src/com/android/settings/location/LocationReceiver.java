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

package com.android.settings.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;

import com.android.settings.R;

import static com.android.settings.location.LocationSettingsBase.KEY_NEVER_ASK;
import static com.android.settings.location.LocationSettingsBase.NEW_MODE_KEY;
import static com.android.settings.location.LocationSettingsBase.PREFERENCE_FILE_NAME;
import static com.android.settings.location.LocationSettingsBase.updateLocationMode;

/**
 * Broadcast receiver invoked when user changes location mode from UI.
 */
public final class LocationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final int currentMode = Secure.getInt(context.getContentResolver(),
                Secure.LOCATION_MODE, Secure.LOCATION_MODE_OFF);
        final int newMode = intent.getIntExtra(NEW_MODE_KEY, currentMode);

        if (shouldShowDialog(context, currentMode, newMode)) {
            Intent dialog = new Intent(context, LocationConfirmDialog.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtras(intent);
            context.startActivity(dialog);
        } else {
            updateLocationMode(context, currentMode, newMode);
        }
    }

    private static boolean shouldShowDialog(Context context, int oldMode, int newMode) {
        if (!context.getResources().getBoolean(R.bool.config_showLocationDialog)) {
            return false;
        }
        if (context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NEVER_ASK, false)) {
            return false;
        }
        return oldMode == Secure.LOCATION_MODE_OFF && oldMode != newMode;
    }
}

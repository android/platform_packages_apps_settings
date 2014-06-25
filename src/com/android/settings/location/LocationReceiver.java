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

import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public final class LocationReceiver extends BroadcastReceiver {
    public static final String INTENT_EXTRA_NEW_MODE = "com.android.settings.location.extra.NEW_MODE";

    @Override
    public void onReceive(Context context, Intent intent) {
        int currentMode = Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
        int newMode = intent.getIntExtra(INTENT_EXTRA_NEW_MODE, currentMode);

        if (LocationConfirmationDialog.shouldShowDialog(context, newMode)) {
            // Show confirmation dialog
            Intent sendIntent = new Intent(context.getApplicationContext(),
                    LocationConfirmationDialog.class);
            sendIntent.putExtra(INTENT_EXTRA_NEW_MODE, newMode);
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            StatusBarManager statusBarManager = (StatusBarManager)context.
                    getApplicationContext().getSystemService(Context.STATUS_BAR_SERVICE);
            if (statusBarManager != null) {
                statusBarManager.collapsePanels();
            }
            context.startActivity(sendIntent);
        } else if (currentMode != newMode) {
            // Change the Location Mode without showing confirmation dialog.
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE, newMode);
        }
    }
}

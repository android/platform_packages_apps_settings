/*
 * Copyright (C) 2018 The Android Open Source Project
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

import com.android.settings.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Listens to {@link Intent.ACTION_PRE_BOOT_COMPLETED}. Non disableable app
 * should not be in the disabled state after SW upgrade.
 */
public class NonDisableableAppSetup extends BroadcastReceiver {

    private static final String TAG = "NonDisableableAppSetup";

    @Override
    public void onReceive(Context context, Intent broadcast) {

        // Change non disableable package setting to its default state
        // Note: This happens if the disabled system app become non disableable after
        // OTA or SW upgrade
        Utils.setNonDisableablePackagesEnabledSetting(context,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP);
    }
}

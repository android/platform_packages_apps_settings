/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import static android.provider.Settings.Secure.CAMERA_LIFT_TRIGGER_ENABLED;
import static android.provider.Settings.Secure.CAMERA_LIFT_TRIGGER_ENABLED_DEFAULT;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

public class CameraLiftTriggerPreferenceController extends GesturePreferenceController {

    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "gesture_camera_lift_trigger_video";
    private final String mCameraLiftTriggerKey;

    private final String SECURE_KEY = CAMERA_LIFT_TRIGGER_ENABLED;

    public CameraLiftTriggerPreferenceController(Context context, String key) {
        super(context, key);
        mCameraLiftTriggerKey = key;
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        return !isGestureAvailable(context)
                || prefs.getBoolean(CameraLiftTriggerSettings.PREF_KEY_SUGGESTION_COMPLETE, false);
    }

    private static boolean isGestureAvailable(Context context) {
        return context.getResources()
                .getBoolean(com.android.internal.R.bool.config_cameraLiftTriggerSensorType) != -1;
    }

    @Override
    public int getAvailabilityStatus() {
        return isGestureAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_camera_lift_trigger");
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isChecked() {
        final int cameraEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                SECURE_KEY, CAMERA_LIFT_TRIGGER_ENABLED_DEFAULT);
        return cameraEnabled == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY,
                isChecked ? ON : OFF);
    }
}



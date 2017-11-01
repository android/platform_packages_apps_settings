/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.internal.app.NightDisplayController;
import com.android.settings.core.PreferenceController;

public class NightDisplayPreferenceController extends PreferenceController {

    private static final String KEY_NIGHT_DISPLAY = "night_display";

    public NightDisplayPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return NightDisplayController.isAvailable(mContext);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NIGHT_DISPLAY;
    }
}

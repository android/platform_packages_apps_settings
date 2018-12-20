/*
 * Copyright 2019 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.emptycategory;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.R;

public class LockScreenSettingsEmptyCategoryEPreferenceController
        extends BasePreferenceController {

    public LockScreenSettingsEmptyCategoryEPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                R.bool.config_security_lockscreen_settings_empty_category_e_visible)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }
}

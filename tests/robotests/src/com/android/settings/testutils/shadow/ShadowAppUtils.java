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

package com.android.settings.testutils.shadow;

import android.content.Context;

import com.android.settingslib.applications.AppUtils;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.Map;

@Implements(AppUtils.class)
public class ShadowAppUtils {

    private static Map<String, String> sAppContentDesMap;

    @Implementation
    protected static CharSequence getAppContentDescription(Context context, String packageName,
            int userId) {
        if (sAppContentDesMap != null) {
            return sAppContentDesMap.get(packageName);
        }
        return null;
    }

    public static void setAppContentDescription(String packageName, String appContentDes) {
        if (sAppContentDesMap == null) {
            sAppContentDesMap = new HashMap<>();
        }
        sAppContentDesMap.put(packageName, appContentDes);
    }
}

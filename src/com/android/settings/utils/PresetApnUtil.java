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

package com.android.settings.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.widget.Toast;

import com.android.settings.R;

public class PresetApnUtil {

    /**
     * Judge whether the APN is preset or not. This judgment uses
     * android.provider.Telephony.Carriers.EDITED. If EDITED==0(UNEDITED), the APN is preset APN,
     * and not added and edited record.
     *
     * @param resolver the content resolver
     * @param key the carriers id of APN
     * @return {@literal true} if the APN is preset.  {@literal false} if not.
     */
    public static boolean isPresetApn(ContentResolver resolver, String key) {
        Cursor cursor = resolver.query(Telephony.Carriers.CONTENT_URI,
                new String[] {Telephony.Carriers.EDITED}, "_id=?", new String[] {key}, null);
        if (cursor != null) {
            try {
                if ((cursor.getCount() > 0)) {
                    cursor.moveToFirst();
                    if (cursor.getInt(0) == Telephony.Carriers.UNEDITED) {
                        return true;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    /**
     * Shows uneditable message.
     *
     * @param context the context
     */
    public static void showMessage(Context context) {
        Toast.makeText(context, context.getString(
                R.string.cannot_change_apn_toast), Toast.LENGTH_LONG).show();
        return;
    }
}

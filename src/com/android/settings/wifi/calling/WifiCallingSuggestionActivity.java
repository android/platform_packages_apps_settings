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

package com.android.settings.wifi.calling;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.settings.SettingsActivity;
import com.android.settings.network.ims.ImsQuery;
import com.android.settings.network.ims.ImsQueryResult;
import com.android.settings.network.ims.ImsQuerySystemTtyStat;
import com.android.settings.network.ims.ImsQueryTtyOnVolteStat;
import com.android.settings.network.ims.ImsQueryWfcUserSetting;
import com.android.settings.network.telephony.MobileNetworkUtils;

public class WifiCallingSuggestionActivity extends SettingsActivity {

    private static final String TAG = "WifiCallingSuggestionActivity";

    public static boolean isSuggestionComplete(Context context) {
        final int subId = SubscriptionManager.getDefaultVoiceSubscriptionId();

        final ImsQuery isWfcEnabledByPlatform =
                MobileNetworkUtils.isWfcEnabledByPlatform(subId);
        final ImsQuery isWfcProvisionedOnDevice =
                MobileNetworkUtils.isWfcProvisionedOnDevice(subId);

        final ImsQuery isWfcEnabledByUser = new ImsQueryWfcUserSetting(subId);
        final ImsQuery isTtyEnabled = new ImsQuerySystemTtyStat(context);
        final ImsQuery isVolteTtyEnabled = new ImsQueryTtyOnVolteStat(subId);

        try (ImsQueryResult queryResult = new ImsQueryResult(
                isWfcEnabledByPlatform, isWfcProvisionedOnDevice, isWfcEnabledByUser,
                isVolteTtyEnabled, isTtyEnabled)) {
            if (queryResult.get(isWfcEnabledByPlatform)
                    && queryResult.get(isWfcProvisionedOnDevice)) {
                return (queryResult.get(isWfcEnabledByUser)
                        && (queryResult.get(isVolteTtyEnabled) || !queryResult.get(isTtyEnabled)));
            }
        } catch (Exception exception) {
            Log.w(TAG, "fail to get Wfc status for suggestion. subId="
                    + subId, exception);
        }
        return true;
    }
}

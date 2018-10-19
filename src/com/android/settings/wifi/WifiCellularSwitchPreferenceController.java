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

package com.android.settings.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.provider.Settings;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

public class WifiCellularSwitchPreferenceController extends AbstractPreferenceController {

    private static final String TAG = "WifiCellularSwitch";
    private static final String KEY_SWITCH_CELLULAR_DATA = "wifi_switch_cellular_data";

    private Context mContext;
    private WifiManager mWifiManager;

    public WifiCellularSwitchPreferenceController(Context context) {
        super(context);
        mContext = context;
        WifiReflectMethod.init();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public boolean isAvailable() {
        return WifiReflectMethod.Settings.System.WIFI_VALID_ENABLE != null;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_SWITCH_CELLULAR_DATA)) {
            return false;
        }
        if (!(preference instanceof SwitchPreference)) {
            return false;
        }
        setWifiValidEnabled(!getWifiValidEnabled());
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SWITCH_CELLULAR_DATA;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        ((SwitchPreference)preference).setChecked(!getWifiValidEnabled());
    }

    private boolean getWifiValidEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
            WifiReflectMethod.Settings.System.WIFI_VALID_ENABLE, 1) == 1;
    }

    private void setWifiValidEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
            WifiReflectMethod.Settings.System.WIFI_VALID_ENABLE,
            enabled ? 1 : 0);

            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (!wifiInfo.getSSID().equals(WifiSsid.NONE)) {
                Log.d(TAG, "Wi-Fi is active, valid enable = " + enabled);
                final ConnectivityManager cm = (ConnectivityManager)
                        mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (enabled) {
                    Log.d(TAG, "Set Wi-Fi always valid, reassociate SSID = " + wifiInfo.getSSID());
                    mWifiManager.reassociate();
                }
                cm.reportNetworkConnectivity(mWifiManager.getCurrentNetwork(), enabled);
            }
    }
}

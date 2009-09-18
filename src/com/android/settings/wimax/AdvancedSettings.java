/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.wimax;

import com.android.settings.R;

import android.net.DhcpInfo;
import android.net.wimax.structs.DeviceInfo;
import android.net.wimax.WimaxManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;

public class AdvancedSettings extends PreferenceActivity {

    private static final String KEY_MAC_ADDRESS = "mac_address";
    private static final String KEY_HW_VERSION = "hw_version";
    private static final String KEY_SW_VERSION = "sw_version";
    private static final String KEY_IP_ADDRESS = "ip_address";
    private static final String KEY_GATEWAY = "gateway";

    private WimaxManager mWimaxManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWimaxManager = (WimaxManager) getSystemService(WIMAX_SERVICE);
        
        addPreferencesFromResource(R.xml.wimax_advanced_settings);
        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }

    private void refreshAll() {
        refreshDeviceInfo();
        refreshIPInfo();
    }

    private void refreshDeviceInfo() {
        DeviceInfo deviceInfo = mWimaxManager.getDeviceInformation();

        Preference wimaxMacAddressPref = findPreference(KEY_MAC_ADDRESS);
        String macAddress = deviceInfo == null ? null : deviceInfo.getMacAddressString();
        wimaxMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress
                : getString(R.string.status_unavailable));

        Preference wimaxHwVersionPref = findPreference(KEY_HW_VERSION);
        String hwVersion = deviceInfo == null ? null : deviceInfo.getHwVersion().getVersion();
        wimaxHwVersionPref.setSummary(!TextUtils.isEmpty(hwVersion) ? hwVersion
                : getString(R.string.status_unavailable));

        Preference wimaxSwVersionPref = findPreference(KEY_SW_VERSION);
        String swVersion = deviceInfo == null ? null : deviceInfo.getSwVersion().getVersion();
        wimaxSwVersionPref.setSummary(!TextUtils.isEmpty(swVersion) ? swVersion
                : getString(R.string.status_unavailable));
    }

    private void refreshIPInfo() {
    	DhcpInfo dhcpInfo = mWimaxManager.getDhcpInfo();
    	
        Preference wimaxIpAddressPref = findPreference(KEY_IP_ADDRESS);
        String ipAddress = dhcpInfo == null ? null : formatAddress(dhcpInfo.ipAddress);
        wimaxIpAddressPref.setSummary(!TextUtils.isEmpty(ipAddress) ? ipAddress
                : getString(R.string.status_unavailable));

        Preference wimaxGatewayPref = findPreference(KEY_GATEWAY);
        String gateway = dhcpInfo == null ? null : formatAddress(dhcpInfo.gateway);
        wimaxGatewayPref.setSummary(!TextUtils.isEmpty(gateway) ? gateway
                : getString(R.string.status_unavailable));
    }
    
    private String formatAddress(int addr) {
    	StringBuffer buf = new StringBuffer();
        buf.append(addr  & 0xff).append('.').
            append((addr >>>= 8) & 0xff).append('.').
            append((addr >>>= 8) & 0xff).append('.').
            append((addr >>>= 8) & 0xff);
        
        return buf.toString();
    }

}

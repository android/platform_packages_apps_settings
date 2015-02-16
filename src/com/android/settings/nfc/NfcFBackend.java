/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.nfc;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.cardemulation.NfcFServiceInfo;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class NfcFBackend {
    public static final String TAG = "Settings.NfcFBackend";

    public static class NfcFAppInfo {
        CharSequence caption;
        Drawable banner;
        public ComponentName componentName;
        String systemCode;
        String nfcid2;
        boolean isRegistered;
        boolean isRegisterable;
    }

    private static final String[] REGISTERED_COMPONENT_KEY_LIST = {
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_1,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_2,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_3,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_4,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_5,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_6,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_7,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_8,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_9,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_10,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_11,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_12,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_13,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_14,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_15,
            Settings.Secure.NFC_NFCF_REGISTERED_COMPONENT_16
    };
    private final Context mContext;
    private final NfcAdapter mAdapter;
    private final CardEmulation mCardEmuManager;

    public NfcFBackend(Context context) {
        mContext = context;

        mAdapter = NfcAdapter.getDefaultAdapter(context);
        mCardEmuManager = CardEmulation.getInstance(mAdapter);
    }

    public List<NfcFAppInfo> getNfcFAppInfos() {
        PackageManager pm = mContext.getPackageManager();
        List<NfcFServiceInfo> serviceInfos =
                mCardEmuManager.getNfcFServices();
        List<NfcFAppInfo> appInfos = new ArrayList<NfcFAppInfo>();

        if (serviceInfos == null) return appInfos;

        for (NfcFServiceInfo service : serviceInfos) {
            NfcFAppInfo appInfo = new NfcFAppInfo();
            appInfo.banner = service.loadBanner(pm);
            appInfo.caption = service.getDescription();
            if (appInfo.caption == null) {
                appInfo.caption = service.loadLabel(pm);
            }
            appInfo.componentName = service.getComponent();
            appInfo.systemCode = service.getSystemCode();
            appInfo.nfcid2 = service.getNfcid2();
            appInfo.isRegistered = isRegisteredApp(appInfo.componentName);
            appInfo.isRegisterable = isRegisterableApp(appInfo.systemCode, appInfo.nfcid2);
            appInfos.add(appInfo);
        }

        return appInfos;
    }

    boolean isForegroundMode() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.NFC_NFCF_FOREGROUND) != 0;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    void setForegroundMode(boolean foreground) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.NFC_NFCF_FOREGROUND, foreground ? 1 : 0) ;
    }

    List<ComponentName> getRegisteredNfcFApp() {
        List<ComponentName> componentsName = new ArrayList<ComponentName>();
        String componentString;
        for (String key : REGISTERED_COMPONENT_KEY_LIST) {
            componentString = Settings.Secure.getString(
                    mContext.getContentResolver(), key);
            if (componentString != null) {
                componentsName.add(
                        ComponentName.unflattenFromString(componentString));
            }
        }

        return componentsName;
    }

    void setRegisteredNfcFApp(ComponentName app, boolean isAdd) {
        String componentString;
        int i;

        if (isAdd) {
            for (i = 0; i < REGISTERED_COMPONENT_KEY_LIST.length; i++) {
                componentString = Settings.Secure.getString(
                        mContext.getContentResolver(),
                        REGISTERED_COMPONENT_KEY_LIST[i]);
                if (componentString == null) {
                    Settings.Secure.putString(
                            mContext.getContentResolver(),
                            REGISTERED_COMPONENT_KEY_LIST[i],
                            app.flattenToString());
                    break;
                }
            }
            if (i == REGISTERED_COMPONENT_KEY_LIST.length) {
                Log.e(TAG, "Couldn't register NfcF App.");
            }
        } else {
            for (i = 0; i < REGISTERED_COMPONENT_KEY_LIST.length; i++) {
                componentString = Settings.Secure.getString(
                        mContext.getContentResolver(),
                        REGISTERED_COMPONENT_KEY_LIST[i]);
                if (componentString != null) {
                    if (componentString.equals(app.flattenToString())) {
                        Settings.Secure.putString(
                                mContext.getContentResolver(),
                                REGISTERED_COMPONENT_KEY_LIST[i],
                                null);
                        break;
                    }
                }
            }
            if (i == REGISTERED_COMPONENT_KEY_LIST.length) {
                Log.e(TAG, "Couldn't deregister NfcF App.");
            }
        }
    }

    private boolean isRegisteredApp(ComponentName componentName) {
        List<ComponentName> registeredNfcFComponentsName =
                getRegisteredNfcFApp();

        if (registeredNfcFComponentsName == null) {
            return false;
        }

        for (ComponentName registeredComponentName : registeredNfcFComponentsName) {
            if (registeredComponentName.flattenToString().equals(
                    componentName.flattenToString())) {
                return true;
            }
        }

        return false;
    }

    private boolean isRegisterableApp(String systemCode, String nfcid2) {
        int numOfRegisteredSystemCode = 0;
        List<ComponentName> registeredNfcFComponentsName = getRegisteredNfcFApp();
        List<NfcFServiceInfo> serviceInfos = mCardEmuManager.getNfcFServices();

        if (registeredNfcFComponentsName == null ||
                serviceInfos == null) {
            return false;
        }

        for (ComponentName registeredComponentName : registeredNfcFComponentsName) {
            for (NfcFServiceInfo service : serviceInfos) {
                if (registeredComponentName.flattenToString().equals(
                        service.getComponent().flattenToString())) {
                    numOfRegisteredSystemCode++;
                    // check if same System Code is already registered
                    if (service.getSystemCode().equals(systemCode)) {
                        return false;
                    }
                    // check if same NFCID2 is already registered
                    if (service.getNfcid2().equals(nfcid2)) {
                        return false;
                    }
                }
            }
        }
        if (numOfRegisteredSystemCode
                >= mCardEmuManager.getMaxNumOfRegisterableSystemCode()) {
            return false;
        }

        return true;
    }
}

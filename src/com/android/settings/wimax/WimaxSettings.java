/*
 * Copyright (C) 2007 The Android Open Source Project
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

import com.android.settings.ProgressCategory;
import com.android.settings.R;
import com.android.settings.wimax.AdvancedSettings;

import android.content.Intent;
import android.net.wimax.WimaxManager;
import android.net.wimax.structs.NSPInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.util.WeakHashMap;

/**
 * Settings screen for Wimax. This will be launched from the main system settings.
 */
public class WimaxSettings extends PreferenceActivity implements WimaxLayer.Callback {

    private static final String TAG = "WimaxSettings";

    //============================
    // Preference/activity member variables
    //============================

    private static final int CONTEXT_MENU_ID_CONNECT = Menu.FIRST;
    private static final int CONTEXT_MENU_ID_DISCONNECT = Menu.FIRST + 1;

    private static final int MENU_ID_SCAN = Menu.FIRST;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 1;

    private static final String KEY_WIMAX_ENABLED = "wimax_enabled";
    private static final String KEY_AUTO_CONNECT_ENABLED = "auto_connect_enabled";
    private static final String KEY_WIMAX_NETWORKS = "wimax_networks";

    private ProgressCategory mNetworksCategory;
    private CheckBoxPreference mWimaxEnabled;
    private WimaxEnabler mWimaxEnabler;
    private CheckBoxPreference mAutoConnectEnabled;

    private WeakHashMap<String, Preference> mPrefs;

    private WimaxLayer mWimaxLayer;

    //============================
    // Activity lifecycle
    //============================

    public WimaxSettings() {
        mPrefs = new WeakHashMap<String, Preference>();
        mWimaxLayer = new WimaxLayer(this, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onCreatePreferences();
        mWimaxLayer.onCreate();
        mWimaxLayer.onCreatedCallback();
    }

    private void onCreatePreferences() {
        addPreferencesFromResource(R.xml.wimax_settings);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        mNetworksCategory = (ProgressCategory) preferenceScreen.findPreference(KEY_WIMAX_NETWORKS);

        mWimaxEnabled = (CheckBoxPreference) preferenceScreen.findPreference(KEY_WIMAX_ENABLED);
        mWimaxEnabler = new WimaxEnabler(this, (WimaxManager) getSystemService(WIMAX_SERVICE),
                mWimaxEnabled);
        mWimaxEnabler.setWimaxLayer(mWimaxLayer);

        mAutoConnectEnabled = (CheckBoxPreference) preferenceScreen
                .findPreference(KEY_AUTO_CONNECT_ENABLED);
        mAutoConnectEnabled.setChecked(Settings.Secure.getInt(getContentResolver(),
            Settings.Secure.WIMAX_AUTO_CONNECT_ON, 0) == 1);

        registerForContextMenu(getListView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWimaxLayer.onResume();
        mWimaxEnabler.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWimaxLayer.onPause();
        mWimaxEnabler.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_ID_SCAN, 0, R.string.scan_wimax)
            .setIcon(R.drawable.ic_menu_scan_network);

        menu.add(0, MENU_ID_ADVANCED, 0, R.string.wimax_menu_advanced)
        .setIcon(android.R.drawable.ic_menu_manage);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {

            case MENU_ID_SCAN:
                mWimaxLayer.attemptWideScan();
                return true;

            case MENU_ID_ADVANCED:
                Intent intent = new Intent(this, AdvancedSettings.class);
                startActivity(intent);
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        String nspName = getNspNameFromMenuInfo(menuInfo);
        if (nspName == null) {
            return;
        }

        menu.setHeaderTitle(nspName);

        if(mWimaxLayer.getCurrentNspName() != null && mWimaxLayer.getCurrentNspName().equalsIgnoreCase(nspName)) {
            menu.add(0, CONTEXT_MENU_ID_DISCONNECT, 1, R.string.wimax_context_menu_disconnect);
        }else {
            menu.add(0, CONTEXT_MENU_ID_CONNECT, 0, R.string.wimax_context_menu_connect);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        String nspName = getNspNameFromMenuInfo(item.getMenuInfo());

        if (nspName == null) {
            return false;
        }

        switch (item.getItemId()) {

            case CONTEXT_MENU_ID_CONNECT:
                mWimaxLayer.connectToNetwork(nspName);
                return true;

            case CONTEXT_MENU_ID_DISCONNECT:
                mWimaxLayer.disconnectFromNetwork();
                return true;

            default:
                return false;
        }
    }

    private String getNspNameFromMenuInfo(ContextMenuInfo menuInfo) {
        if ((menuInfo == null) || !(menuInfo instanceof AdapterContextMenuInfo)) {
            return null;
        }

        AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
        if(adapterMenuInfo.position < 4) {  //skip the first two menu items.
            return null;
        }
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(
                adapterMenuInfo.position);
        if (pref == null || !(pref instanceof Preference)) {
            return null;
        }

        if(pref.getTitle() == null || pref.getTitle().length() == 0)
            return null;
        else
            return String.valueOf(pref.getTitle());
    }

    private int getSignalStrength(int rssi) {
        int level = WimaxManager.calculateSignalLevel(rssi, 4);
        switch(level) {
            case 0: return R.string.wimax_signal_0;
            case 1: return R.string.wimax_signal_1;
            case 2: return R.string.wimax_signal_2;
            case 3: return R.string.wimax_signal_3;
            default: return R.string.status_unavailable;
        }
    }

    //============================
    // Preference callbacks
    //============================

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        if (preference == mAutoConnectEnabled) {
            Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.WIMAX_AUTO_CONNECT_ON,
                mAutoConnectEnabled.isChecked() ? 1 : 0);
        }

        return false;
    }

    //============================
    // Wimax callbacks
    //============================

    public void onError(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show();
    }

    public void onScanningStatusChanged(boolean started) {
        mNetworksCategory.setProgress(started);
    }

    public void onNetworkListChanged(NSPInfo nspInfo, boolean added) {

        String nspName = nspInfo.getNspName();
        Preference pref = mPrefs.get(nspName);

        if (WimaxLayer.LOGV) {
            Log.v(TAG, "onNetworkListChanged with " + nspName + " and "
                    + (added ? "added" : "removed"));
        }

        if (added) {
            if (pref == null) {
                pref = new Preference(this);
                pref.setTitle(nspName);
                mPrefs.put(nspName, pref);
            } else {
                pref.setEnabled(true);
            }
            pref.setSummary(getSignalStrength(nspInfo.getRssiInDBm()));

            mNetworksCategory.addPreference(pref);
        } else {
            mPrefs.remove(nspName);

            if (pref != null) {
                mNetworksCategory.removePreference(pref);
            }
        }
    }

    public void onWimaxStatusChanged(boolean enabled) {
        if (enabled) {
            mNetworksCategory.setEnabled(true);
        } else {
            mNetworksCategory.removeAll();
            mPrefs.clear();
        }
    }
}

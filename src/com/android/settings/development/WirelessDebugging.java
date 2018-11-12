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

package com.android.settings.development;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settingslib.search.SearchIndexable;

// TODO(joshuaduong): remove once AdbWirelessManager is in.
import com.android.settings.development.tests.AdbWirelessManager;

@SearchIndexable
public class WirelessDebugging extends DashboardFragment
        implements Indexable {

    private static final String TAG = "WirelessDebugging";

    private WirelessDebuggingEnabler mWifiDebuggingEnabler;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferences();
    }

    private void addPreferences() {
        addPreferencesFromResource(R.xml.adb_wireless_settings);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mWifiDebuggingEnabler != null) {
            mWifiDebuggingEnabler.teardownSwitchController();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mWifiDebuggingEnabler = createWirelessDebuggingEnabler();
        onAdbWirelessStateChanged(AdbWirelessManager.getAdbWirelessState());
    }

    @Override
    public void onResume() {
        final Activity activity = getActivity();
        super.onResume();

        if (mWifiDebuggingEnabler != null) {
            mWifiDebuggingEnabler.resume(activity);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mWifiDebuggingEnabler != null) {
            mWifiDebuggingEnabler.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_ADB_WIRELESS;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onDialogShowing() {
        super.onDialogShowing();
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
       return R.xml.adb_wireless_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    //@Override
    public void onAdbWirelessStateChanged(int state) {
        switch (state) {
            case AdbWirelessManager.ADB_WIRELESS_STATE_DISABLED:
                break;
            case AdbWirelessManager.ADB_WIRELESS_STATE_DISABLING:
                break;
            case AdbWirelessManager.ADB_WIRELESS_STATE_ENABLED:
                break;
            case AdbWirelessManager.ADB_WIRELESS_STATE_ENABLING:
                break;
            default:
                break;
        }
    }

    /**
     * @return new WirelessDebuggingEnabler or null
     */
    private WirelessDebuggingEnabler createWirelessDebuggingEnabler() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        return new WirelessDebuggingEnabler(activity,
                        new SwitchBarController(activity.getSwitchBar()));
    }
}

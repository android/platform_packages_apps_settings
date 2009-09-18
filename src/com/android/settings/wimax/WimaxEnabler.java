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

import static android.net.wimax.WimaxManager.WIMAX_DISABLED;
import static android.net.wimax.WimaxManager.WIMAX_DISABLING;
import static android.net.wimax.WimaxManager.WIMAX_ENABLED;
import static android.net.wimax.WimaxManager.WIMAX_ENABLING;
import static android.net.wimax.WimaxManager.WIMAX_STATUS_UNKNOWN;

import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wimax.WimaxManager;
import android.net.wimax.WimaxState;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;

public class WimaxEnabler implements Preference.OnPreferenceChangeListener {

    private static final boolean LOCAL_LOGD = Config.LOGD || WimaxLayer.LOGV;
    private static final String TAG = "SettingsWimaxEnabler";

    private final Context mContext;
    private final WimaxManager mWimaxManager;
    private final CheckBoxPreference mWimaxCheckBoxPref;
    private final CharSequence mOriginalSummary;
    private WimaxLayer mWimaxLayer;

    private final IntentFilter mWimaxStatusFilter;
    private final BroadcastReceiver mWimaxStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WimaxManager.WIMAX_STATUS_CHANGED_ACTION)) {
                handleWimaxStatusChanged(
                        intent.getIntExtra(WimaxManager.EXTRA_WIMAX_STATUS, WIMAX_STATUS_UNKNOWN),
                        intent.getIntExtra(WimaxManager.EXTRA_PREVIOUS_WIMAX_STATUS,
                                WIMAX_STATUS_UNKNOWN));
            } else if (intent.getAction().equals(WimaxManager.WIMAX_STATE_CHANGED_ACTION)) {
                handleWimaxStateChanged(
                        (WimaxState) intent.getParcelableExtra(WimaxManager.EXTRA_WIMAX_STATE));
            }
        }
    };

    public WimaxEnabler(Context context, WimaxManager wimaxManager,
            CheckBoxPreference wimaxCheckBoxPreference) {
        mContext = context;
        mWimaxCheckBoxPref = wimaxCheckBoxPreference;
        mWimaxManager = wimaxManager;

        mOriginalSummary = wimaxCheckBoxPreference.getSummary();
        wimaxCheckBoxPreference.setPersistent(false);

        mWimaxStatusFilter = new IntentFilter(WimaxManager.WIMAX_STATUS_CHANGED_ACTION);
        mWimaxStatusFilter.addAction(WimaxManager.WIMAX_STATE_CHANGED_ACTION);
    }

    public void resume() {
        int status = mWimaxManager.getWimaxStatus();
        // This is the widget enabled status, not the preference toggled status
        mWimaxCheckBoxPref.setEnabled(status == WIMAX_ENABLED || status == WIMAX_DISABLED
                || status == WIMAX_STATUS_UNKNOWN);

        mContext.registerReceiver(mWimaxStatusReceiver, mWimaxStatusFilter);
        mWimaxCheckBoxPref.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        mContext.unregisterReceiver(mWimaxStatusReceiver);
        mWimaxCheckBoxPref.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn on/off Wimax
        setWimaxEnabled((Boolean) value);

        // Don't update UI to opposite status until we're sure
        return false;
    }

    private void setWimaxEnabled(final boolean enable) {
        // Disable button
        mWimaxCheckBoxPref.setEnabled(false);

        if (!mWimaxManager.setWimaxEnabled(enable)) {
            mWimaxCheckBoxPref.setSummary(enable ? R.string.error_wimax_starting : R.string.error_wimax_stopping);
        }
    }

    public void setWimaxLayer(WimaxLayer wimaxLayer) {
        mWimaxLayer = wimaxLayer;
    }

    private void handleWimaxStatusChanged(int wimaxStatus, int previousWimaxStatus) {

        if (LOCAL_LOGD) {
            Log.d(TAG, "Received wimax status changed from "
                    + getHumanReadableWimaxStatus(previousWimaxStatus) + " to "
                    + getHumanReadableWimaxStatus(wimaxStatus));
        }

        if (wimaxStatus == WIMAX_DISABLED || wimaxStatus == WIMAX_ENABLED) {
            mWimaxCheckBoxPref.setChecked(wimaxStatus == WIMAX_ENABLED);
            mWimaxCheckBoxPref
                    .setSummary(wimaxStatus == WIMAX_DISABLED ? mOriginalSummary : null);

            mWimaxCheckBoxPref.setEnabled(isEnabledByDependency());

        } else if (wimaxStatus == WIMAX_DISABLING || wimaxStatus == WIMAX_ENABLING) {
            mWimaxCheckBoxPref.setSummary(wimaxStatus == WIMAX_ENABLING ? R.string.wimax_starting
                    : R.string.wimax_stopping);

        } else if (wimaxStatus == WIMAX_STATUS_UNKNOWN) {
            int message = R.string.wimax_error;
            if (previousWimaxStatus == WIMAX_ENABLING) message = R.string.error_wimax_starting;
            else if (previousWimaxStatus == WIMAX_DISABLING) message = R.string.error_wimax_stopping;

            mWimaxCheckBoxPref.setChecked(false);
            mWimaxCheckBoxPref.setSummary(message);
            mWimaxCheckBoxPref.setEnabled(true);
        }
    }

    private void handleWimaxStateChanged(WimaxState state) {

        if (LOCAL_LOGD) {
            Log.d(TAG, "Received wimax state changed to " + state);
        }

        if (mWimaxManager.isWimaxEnabled()) {
            mWimaxCheckBoxPref.setSummary(getPrintableSummary(state));
        }
    }

    private boolean isEnabledByDependency() {
        Preference dep = getDependencyPreference();
        if (dep == null) {
            return true;
        }

        return !dep.shouldDisableDependents();
    }

    private Preference getDependencyPreference() {
        String depKey = mWimaxCheckBoxPref.getDependency();
        if (TextUtils.isEmpty(depKey)) {
            return null;
        }

        return mWimaxCheckBoxPref.getPreferenceManager().findPreference(depKey);
    }

    private static String getHumanReadableWimaxStatus(int wimaxStatus) {
        switch (wimaxStatus) {
            case WIMAX_DISABLED:
                return "Disabled";
            case WIMAX_DISABLING:
                return "Disabling";
            case WIMAX_ENABLED:
                return "Enabled";
            case WIMAX_ENABLING:
                return "Enabling";
            case WIMAX_STATUS_UNKNOWN:
                return "Unknown";
            default:
                return "Some other state!";
        }
    }

    private String getPrintableSummary(WimaxState wimaxState) {
        String summary = null;
        if(wimaxState == WimaxState.SCANNING) {
            summary = mContext.getString(R.string.wimax_status_scanning);
        }else if(wimaxState == WimaxState.CONNECTING) {
            String nspName = mWimaxLayer != null ?mWimaxLayer.getNspToConnect() :null;
            if(nspName == null || "".equals(nspName))
                summary = mContext.getString(R.string.wimax_status_connecting);
            else
                summary = String.format(mContext.getString(R.string.wimax_status_connecting_to), nspName);
        }if(wimaxState == WimaxState.CONNECTED) {
            String nspName = mWimaxLayer != null ?mWimaxLayer.getCurrentNspName() :null;
            if(nspName == null || "".equals(nspName))
                summary = mContext.getString(R.string.wimax_status_connected);
            else
                summary = String.format(mContext.getString(R.string.wimax_status_connected_to), nspName);
        }if(wimaxState == WimaxState.DISCONNECTING) {
            String nspName = mWimaxLayer != null ?mWimaxLayer.getCurrentNspName() :null;
            if(nspName == null || "".equals(nspName))
                summary = mContext.getString(R.string.wimax_status_disconnecting);
            else
                summary = String.format(mContext.getString(R.string.wimax_status_disconnecting_from), nspName);
        }if(wimaxState == WimaxState.DISCONNECTED) {
            summary = mContext.getString(R.string.wimax_status_disconnected);
        }

        return summary;
    }
}

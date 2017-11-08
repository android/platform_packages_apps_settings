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

package com.android.settings.wifi.calling;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

/**
 * Interface to control disclaimer item from {@link DisclaimerActivity}.
 */
@VisibleForTesting
public abstract class DisclaimerItem {
    private static final String SHARED_PREFERENCES_NAME = "wfc_disclaimer_prefs";

    protected final Context mContext;
    protected final int mSubId;
    private final View mDisclaimerView;

    DisclaimerItem(Context context, int subId) {
        mContext = context;
        mSubId = subId;
        mDisclaimerView = createView();
    }

    /**
     * Called by the {@link DisclaimerActivity} when a user has clicked the agree button.
     */
    void onAgreed() {
        setBooleanSharedPrefs(getPrefKey(), true);
    }

    /**
     * Gets the {@link View} instance for displaying disclaimer information.
     *
     * @return The {@link View} instance.
     */
    View getView() {
        return mDisclaimerView;
    }

    /**
     * Checks whether the disclaimer item need to be displayed or not.
     *
     * @return Returns {@code true} if disclaimer item need to be displayed,
     * {@code false} if not displayed.
     */
    boolean shouldShow() {
        if (getBooleanSharedPrefs(getPrefKey(), false)) {
            logd("shouldShow: false due to a user has already agreed.");
            return false;
        }
        logd("shouldShow: true");
        return true;
    }

    /**
     * Gets the configuration values for a particular phone id.
     *
     * @return The {@link PersistableBundle} instance containing the config value for a
     * particular phone id, or default values.
     */
    protected PersistableBundle getCarrierConfig() {
        CarrierConfigManager configManager = (CarrierConfigManager) mContext.getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            // If an invalid subId is used, the returned config will contain default values.
            PersistableBundle config = configManager.getConfigForSubId(mSubId);
            if (config != null) {
                return config;
            }
        }
        // Return static default defined in CarrierConfigManager.
        return CarrierConfigManager.getDefaultConfig();
    }

    protected void logd(String msg) {
        Log.d(getName(), "[" + mSubId +  "] " + msg);
    }

    /**
     * Gets a message id for disclaimer item.
     *
     * @return Message id for disclaimer item.
     */
    protected abstract int getMessageId();

    /**
     * Gets a name of disclaimer item.
     *
     * @return Name of disclaimer item.
     */
    protected abstract String getName();

    /**
     * Gets a preference key to keep user's consent.
     *
     * @return Preference key to keep user's consent.
     */
    protected abstract String getPrefKey();

   /**
    * Creates the {@link View} instance for displaying disclaimer information.
    *
    * @return The {@link View} instance.
    */
    private View createView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.wfc_simple_disclaimer_item, null, false);
        TextView textView = (TextView) view.findViewById(R.id.disclaimer_desc);
        textView.setText(getMessageId());
        return view;
    }

    /**
     * Gets the boolean value from shared preferences.
     *
     * @param key The key for the preference item.
     * @param defValue Value to return if this preference does not exist.
     * @return The boolean value of corresponding key, or defValue.
     */
    private boolean getBooleanSharedPrefs(String key, boolean defValue) {
        SharedPreferences prefs = mContext.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return prefs.getBoolean(key + mSubId, defValue);
    }

    /**
     * Sets the boolean value to shared preferences.
     *
     * @param key The key for the preference item.
     * @param value The value to be set for shared preferences.
     */
    private void setBooleanSharedPrefs(String key, boolean value) {
        SharedPreferences prefs = mContext.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key + mSubId, value).apply();
    }
}

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

package com.android.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.ims.ImsManager;
import com.android.settings.R;

/**
 * Preference for showing Wi-Fi Calling privacy policy UI regarding location information.
 */
public class WifiCallingPrivacyPolicyPreference extends Preference implements OnClickListener {
    private static final String TAG = "WifiCallingPrivacyPolicyPreference";
    private static final String SHARED_PREFERENCES_NAME = "wfc_privacy_policy_prefs";

    /*
     * Preference key for whether a user already agreed disclaimer.
     * if true, not showing Wi-Fi Calling privacy policy UI next time.
     */
    private static final String KEY_HAS_AGREED_FOR_DISCLAIMER = "key_has_agreed_for_disclaimer";

    private final Context mContext;
    private Listener mListener;

    public interface Listener {
        void onAgreed(Preference preference);
        void onDisagreed(Preference preference);
    }

    /**
     * Constructor that is called when there should show Wi-Fi Calling privacy pilicy UI.
     *
     * @param context The context.
     */
    public WifiCallingPrivacyPolicyPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.wifi_calling_privacy_policy);

        mContext = context;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        Button agreeButton = (Button) holder.findViewById(R.id.agree_button);
        agreeButton.setOnClickListener(this);
        Button disagreeButton = (Button) holder.findViewById(R.id.disagree_button);
        disagreeButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.agree_button:
                Log.d(TAG, "WifiCallingPrivacyPolicyPreference: onAgreed");

                // As user has clicked agree button, Setting KEY_HAS_AGREED_FOR_DISCLAIMER
                // with true for not showing Wi-Fi Calling privacy policy UI next time.
                SharedPreferences prefs = mContext.getSharedPreferences(
                        SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_HAS_AGREED_FOR_DISCLAIMER, true).apply();
                if (mListener != null) {
                    mListener.onAgreed(this);
                }
                break;
            case R.id.disagree_button:
                Log.d(TAG, "WifiCallingPrivacyPolicyPreference: onDisagreed");
                if (mListener != null) {
                    mListener.onDisagreed(this);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Set the callback to be invoked when the user responds to the privacy policy disclaimer.
     *
     * @param listener The callback to be invoked.
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    /**
     * Whether the Wi-Fi Calling privacy pilicy UI should be shown.
     *
     * @param context The context.
     * @return Returns {@code true} if an {@link WifiCallingPrivacyPolicyPreference}
     *         should be shown.
     */
    public static boolean shouldShow(Context context) {
        boolean showPrivacyPolicy = getBooleanCarrierConfig(context,
                CarrierConfigManager.KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL);

        SharedPreferences prefs = context.getSharedPreferences(
                SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean hasAgreed = prefs.getBoolean(KEY_HAS_AGREED_FOR_DISCLAIMER, false);

        boolean wfcEnabled = ImsManager.isWfcEnabledByUser(context)
                && ImsManager.isNonTtyOrTtyOnVolteEnabled(context);

        boolean defaultWfcEnabled = getBooleanCarrierConfig(context,
                CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL);

        Log.d(TAG, "showPrivacyPolicy = " + showPrivacyPolicy
                + ", wfcEnabled = " + wfcEnabled
                + ", hasAgreed = " + hasAgreed
                + ", defaultWfcEnabled = " + defaultWfcEnabled);

        return showPrivacyPolicy && !hasAgreed && !wfcEnabled && !defaultWfcEnabled;
    }

    private static boolean getBooleanCarrierConfig(Context context, String key) {
        CarrierConfigManager configManager = (CarrierConfigManager) context
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            int subId = SubscriptionManager.getDefaultVoicePhoneId();
            // If an invalid subId is used, this bundle will contain default values.
            PersistableBundle config = configManager.getConfigForSubId(subId);
            if (config != null) {
                return config.getBoolean(key);
            }
        }
        // Return static default defined in CarrierConfigManager.
        return CarrierConfigManager.getDefaultConfig().getBoolean(key);
    }
}

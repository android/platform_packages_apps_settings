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

package com.android.settings.network.telephony;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.ims.ImsQuery;
import com.android.settings.network.ims.ImsQueryFeatureIsReady;
import com.android.settings.network.ims.ImsQueryProvisioningStat;
import com.android.settings.network.ims.ImsQueryResult;
import com.android.settings.network.ims.ImsQuerySupportStat;
import com.android.settings.network.ims.ImsQueryWfcUserSetting;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

/**
 * Preference controller for "Wifi Calling"
 */
public class WifiCallingPreferenceController extends TelephonyBasePreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "WifiCallingPreference";

    @VisibleForTesting
    Integer mCallState;
    @VisibleForTesting
    CarrierConfigManager mCarrierConfigManager;
    private ImsMmTelManager mImsMmTelManager;
    @VisibleForTesting
    PhoneAccountHandle mSimCallManager;
    private PhoneCallStateListener mPhoneStateListener;
    private Preference mPreference;

    public WifiCallingPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mPhoneStateListener = new PhoneCallStateListener();
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (mSimCallManager != null) {
            return (getAccountConfigureIntent() != null)
                    ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        }
        try {
            return ImsQueryResult.andAll(isImsServiceStateReady(subId),
                    isWfcProvisionedOnDevice(subId),
                    isWfcEnabledByPlatform(subId))
                    ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        } catch (Exception exception) {
            Log.w(TAG, "fail to get available status. subId=" + subId, exception);
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onStart() {
        mPhoneStateListener.register(mContext, mSubId);
    }

    @Override
    public void onStop() {
        mPhoneStateListener.unregister();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        final Intent intent = mPreference.getIntent();
        if (intent != null) {
            intent.putExtra(Settings.EXTRA_SUB_ID, mSubId);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mCallState == null) {
            return;
        }
        if (mSimCallManager != null) {
            final Intent intent = getAccountConfigureIntent();
            if (intent == null) {
                // Do nothing in this case since preference is invisible
                return;
            }
            final PackageManager pm = mContext.getPackageManager();
            final List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
            preference.setTitle(resolutions.get(0).loadLabel(pm));
            preference.setSummary(null);
            preference.setIntent(intent);
        } else {
            final String title = SubscriptionManager.getResourcesForSubId(mContext, mSubId)
                    .getString(R.string.wifi_calling_settings_title);
            preference.setTitle(title);
            preference.setSummary(getResourceIdForWfcMode(mSubId));
        }
        preference.setEnabled(mCallState == TelephonyManager.CALL_STATE_IDLE);
    }

    private CharSequence getResourceIdForWfcMode(int subId) {
        int resId = com.android.internal.R.string.wifi_calling_off_summary;

        try {
            if (!ImsQueryResult.andAll(isWfcEnabledByUser(subId))) {
                return SubscriptionManager.getResourcesForSubId(mContext, subId).getText(resId);
            }
        } catch (Exception exception) {
            Log.w(TAG, "fail to get Wfc status for summary. subId=" + subId, exception);
            return SubscriptionManager.getResourcesForSubId(mContext, subId).getText(resId);
        }

        boolean useWfcHomeModeForRoaming = false;
        if (mCarrierConfigManager != null) {
            final PersistableBundle carrierConfig =
                    mCarrierConfigManager.getConfigForSubId(subId);
            if (carrierConfig != null) {
                useWfcHomeModeForRoaming = carrierConfig.getBoolean(
                        CarrierConfigManager
                                .KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL);
            }
        }
        final boolean isRoaming = getTelephonyManager(mContext, subId)
                .isNetworkRoaming();
        final int wfcMode = (isRoaming && !useWfcHomeModeForRoaming)
                ? mImsMmTelManager.getVoWiFiRoamingModeSetting() :
                mImsMmTelManager.getVoWiFiModeSetting();
        switch (wfcMode) {
            case ImsMmTelManager.WIFI_MODE_WIFI_ONLY:
                resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                break;
            case ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED:
                resId = com.android.internal.R.string
                        .wfc_mode_cellular_preferred_summary;
                break;
            case ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED:
                resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                break;
            default:
                break;
        }
        return SubscriptionManager.getResourcesForSubId(mContext, subId).getText(resId);
    }

    public WifiCallingPreferenceController init(int subId) {
        mSubId = subId;
        mImsMmTelManager = getImsMmTelManager(mSubId);
        mSimCallManager = mContext.getSystemService(TelecomManager.class)
                .getSimCallManagerForSubscription(mSubId);

        return this;
    }

    @VisibleForTesting
    ImsMmTelManager getImsMmTelManager(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return null;
        }
        return ImsMmTelManager.createForSubscriptionId(subId);
    }

    @VisibleForTesting
    TelephonyManager getTelephonyManager(Context context, int subId) {
        final TelephonyManager telephonyMgr = context.getSystemService(TelephonyManager.class);
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return telephonyMgr;
        }
        final TelephonyManager subscriptionTelephonyMgr =
                telephonyMgr.createForSubscriptionId(subId);
        return (subscriptionTelephonyMgr == null) ? telephonyMgr : subscriptionTelephonyMgr;
    }

    private Intent getAccountConfigureIntent() {
        return MobileNetworkUtils.buildPhoneAccountConfigureIntent(mContext,
                mSimCallManager);
    }

    @VisibleForTesting
    ImsQuery isWfcEnabledByUser(int subId) {
        return new ImsQueryWfcUserSetting(subId);
    }

    @VisibleForTesting
    ImsQuery isImsServiceStateReady(int subId) {
        return new ImsQueryFeatureIsReady(subId);
    }

    @VisibleForTesting
    ImsQuery isWfcProvisionedOnDevice(int subId) {
        return new ImsQueryProvisioningStat(subId,
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN);
    }

    @VisibleForTesting
    ImsQuery isWfcEnabledByPlatform(int subId) {
        return new ImsQuerySupportStat(subId,
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                AccessNetworkConstants.TRANSPORT_TYPE_WLAN);
    }

    private class PhoneCallStateListener extends PhoneStateListener {

        PhoneCallStateListener() {
            super();
        }

        private TelephonyManager mTelephonyManager;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            mTelephonyManager = getTelephonyManager(context, subId);
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
        }

        public void unregister() {
            mCallState = null;
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
        }
    }
}

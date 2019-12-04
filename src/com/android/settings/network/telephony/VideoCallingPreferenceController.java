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
import android.os.PersistableBundle;
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
import androidx.preference.SwitchPreference;

import com.android.settings.network.MobileDataEnabledListener;
import com.android.settings.network.ims.ImsQuery;
import com.android.settings.network.ims.ImsQueryEnhanced4gLteModeUserSetting;
import com.android.settings.network.ims.ImsQueryFeatureIsReady;
import com.android.settings.network.ims.ImsQueryMobileDataSetting;
import com.android.settings.network.ims.ImsQueryProvisioningStat;
import com.android.settings.network.ims.ImsQueryResult;
import com.android.settings.network.ims.ImsQuerySupportStat;
import com.android.settings.network.ims.ImsQuerySystemTtyStat;
import com.android.settings.network.ims.ImsQueryTtyOnVolteStat;
import com.android.settings.network.ims.ImsQueryVtUserSetting;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "Video Calling"
 */
public class VideoCallingPreferenceController extends TelephonyTogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop,
        MobileDataEnabledListener.Client,
        Enhanced4gBasePreferenceController.On4gLteUpdateListener {

    private static final String TAG = "VideoCallingPreference";

    private Preference mPreference;
    private CarrierConfigManager mCarrierConfigManager;
    private PhoneCallStateListener mPhoneStateListener;
    @VisibleForTesting
    Integer mCallState;
    private MobileDataEnabledListener mDataContentObserver;

    public VideoCallingPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mDataContentObserver = new MobileDataEnabledListener(context, this);
        mPhoneStateListener = new PhoneCallStateListener();
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && isVideoCallEnabled(subId)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mPhoneStateListener.register(mContext, mSubId);
        mDataContentObserver.start(mSubId);
    }

    @Override
    public void onStop() {
        mPhoneStateListener.unregister();
        mDataContentObserver.stop();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mCallState == null) {
            return;
        }
        final SwitchPreference switchPreference = (SwitchPreference) preference;

        final boolean videoCallEnabled = isVideoCallEnabled(mSubId);
        switchPreference.setVisible(videoCallEnabled);
        if (!videoCallEnabled) {
            return;
        }

        final ImsQuery isTtyEnabled = isSystemTtyEnabled();
        final ImsQuery isVolteTtyEnabled = isTtyOnVolteEnabled(mSubId);
        final ImsQuery is4gLteSetting = isEnhanced4gLteModeSettingEnabledByUser(mSubId);
        final ImsQuery isVtSettingEnabled = isVtEnabledByUser(mSubId);

        try (ImsQueryResult queryResult = new ImsQueryResult(
                is4gLteSetting, isVtSettingEnabled,
                isVolteTtyEnabled, isTtyEnabled)) {
            final boolean is4gLteEnabled = queryResult.get(is4gLteSetting)
                    && (queryResult.get(isVolteTtyEnabled) || !queryResult.get(isTtyEnabled));
            preference.setEnabled(is4gLteEnabled &&
                    mCallState == TelephonyManager.CALL_STATE_IDLE);
            switchPreference.setChecked(is4gLteEnabled && queryResult.get(isVtSettingEnabled));
        } catch (Exception exception) {
            Log.w(TAG, "fail to get VT status before updating. subId=" + mSubId, exception);
            preference.setEnabled(false);
            switchPreference.setChecked(false);
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return false;
        }
        final ImsMmTelManager imsMmTelManager = ImsMmTelManager.createForSubscriptionId(mSubId);
        if (imsMmTelManager == null) {
            return false;
        }
        imsMmTelManager.setVtSettingEnabled(isChecked);
        return true;
    }

    @Override
    public boolean isChecked() {
        try {
            return ImsQueryResult.andAll(isVtEnabledByUser(mSubId));
        } catch (Exception exception) {
            Log.w(TAG, "fail to get VT setting status. subId=" + mSubId, exception);
        }
        return false;
    }

    public VideoCallingPreferenceController init(int subId) {
        mSubId = subId;
        return this;
    }

    @VisibleForTesting
    ImsQuery isMobileDataEnabled(int subId) {
        return new ImsQueryMobileDataSetting(mContext, subId);
    }

    @VisibleForTesting
    ImsQuery isSystemTtyEnabled() {
        return new ImsQuerySystemTtyStat(mContext);
    }

    @VisibleForTesting
    ImsQuery isTtyOnVolteEnabled(int subId) {
        return new ImsQueryTtyOnVolteStat(subId);
    }

    @VisibleForTesting
    ImsQuery isEnhanced4gLteModeSettingEnabledByUser(int subId) {
        return new ImsQueryEnhanced4gLteModeUserSetting(subId);
    }

    @VisibleForTesting
    ImsQuery isVtEnabledByUser(int subId) {
        return new ImsQueryVtUserSetting(subId);
    }

    @VisibleForTesting
    ImsQuery isImsServiceStateReady(int subId) {
        return new ImsQueryFeatureIsReady(subId);
    }

    @VisibleForTesting
    ImsQuery isVtProvisionedOnDevice(int subId) {
        return new ImsQueryProvisioningStat(subId,
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                ImsRegistrationImplBase.REGISTRATION_TECH_LTE);
    }

    @VisibleForTesting
    ImsQuery isVtEnabledByPlatform(int subId) {
        return new ImsQuerySupportStat(subId,
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
    }

    @VisibleForTesting
    boolean isVideoCallEnabled(int subId) {
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);
        if ((carrierConfig == null)
                || (!carrierConfig.getBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS))) {
            return false;
        }

        try {
            return ImsQueryResult.andAll(isMobileDataEnabled(subId),
                    isImsServiceStateReady(subId),
                    isVtEnabledByPlatform(subId),
                    isVtProvisionedOnDevice(subId));
        } catch (Exception exception) {
            Log.w(TAG, "fail to get VT enable status. subId=" + subId, exception);
        }
        return false;
    }

    @Override
    public void on4gLteUpdated() {
        updateState(mPreference);
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
            mTelephonyManager = context.getSystemService(TelephonyManager.class);
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
            }
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
        }

        public void unregister() {
            mCallState = null;
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
        }
    }

    /**
     * Implementation of MobileDataEnabledListener.Client
     */
    public void onMobileDataEnabledChange() {
        updateState(mPreference);
    }
}

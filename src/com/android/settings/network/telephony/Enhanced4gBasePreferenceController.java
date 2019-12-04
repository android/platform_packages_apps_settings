/*
 * Copyright (C) 2019 The Android Open Source Project
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

import com.android.settings.network.ims.ImsQuery;
import com.android.settings.network.ims.ImsQueryEnhanced4gLteModeUserSetting;
import com.android.settings.network.ims.ImsQueryFeatureIsReady;
import com.android.settings.network.ims.ImsQueryProvisioningStat;
import com.android.settings.network.ims.ImsQueryResult;
import com.android.settings.network.ims.ImsQuerySupportStat;
import com.android.settings.network.ims.ImsQuerySystemTtyStat;
import com.android.settings.network.ims.ImsQueryTtyOnVolteStat;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller for "Enhanced 4G LTE"
 */
public class Enhanced4gBasePreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "Enhanced4g";

    @VisibleForTesting
    Preference mPreference;
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mCarrierConfig;
    private PhoneCallStateListener mPhoneStateListener;
    @VisibleForTesting
    Integer mCallState;
    private final List<On4gLteUpdateListener> m4gLteListeners;

    protected static final int MODE_NONE = -1;
    protected static final int MODE_VOLTE = 0;
    protected static final int MODE_ADVANCED_CALL = 1;
    protected static final int MODE_4G_CALLING = 2;
    private int m4gCurrentMode = MODE_NONE;

    public Enhanced4gBasePreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        m4gLteListeners = new ArrayList<>();
        mPhoneStateListener = new PhoneCallStateListener();
    }

    public Enhanced4gBasePreferenceController init(int subId) {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && mSubId == subId) {
            return this;
        }
        mSubId = subId;
        mCarrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);

        final boolean show4GForLTE = mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL);
        m4gCurrentMode = mCarrierConfig.getInt(
                CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT);
        if (m4gCurrentMode != MODE_ADVANCED_CALL) {
            m4gCurrentMode = show4GForLTE ? MODE_4G_CALLING : MODE_VOLTE;
        }
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        init(subId);
        if (!isModeMatched()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);

        final boolean isConfigOn = subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && carrierConfig != null
                && !carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL);
        if (!isConfigOn) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        final ImsQuery isServiceStateReady = isImsServiceStateReady(subId);
        final ImsQuery isTtyEnabled = isSystemTtyEnabled();
        final ImsQuery isVolteTtyEnabled = isTtyOnVolteEnabled(subId);
        final ImsQuery isVolteProvisioned = isVolteProvisionedOnDevice(subId);
        final ImsQuery isVoltePlatformEnabled = isVolteEnabledByPlatform(subId);

        try (ImsQueryResult queryResult = new ImsQueryResult(
                isVoltePlatformEnabled, isServiceStateReady, isVolteProvisioned,
                isVolteTtyEnabled, isTtyEnabled)) {
            if (queryResult.get(isVoltePlatformEnabled)
                    && queryResult.get(isServiceStateReady)
                    && queryResult.get(isVolteProvisioned)) {
                return (isPrefEnabled()
                        && (queryResult.get(isVolteTtyEnabled) || !queryResult.get(isTtyEnabled)))
                        ? AVAILABLE : AVAILABLE_UNSEARCHABLE;
            }
        } catch (Exception exception) {
            Log.w(TAG, "fail to get available status. subId=" + subId, exception);
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
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
    public void updateState(Preference preference) {
        super.updateState(preference);
        final SwitchPreference switchPreference = (SwitchPreference) preference;

        final boolean isEnabled = isPrefEnabled();
        boolean isChecked = false;
        boolean isNonTtyOrTtyOnVolteEnabled = false;

        final ImsQuery isTtyEnabled = isSystemTtyEnabled();
        final ImsQuery isVolteTtyEnabled = isTtyOnVolteEnabled(mSubId);
        final ImsQuery isEnhanced4gLteModeSettingEnabled =
                isEnhanced4gLteModeSettingEnabledByUser(mSubId);

        try (ImsQueryResult queryResult = new ImsQueryResult(
                isEnhanced4gLteModeSettingEnabled,
                isVolteTtyEnabled, isTtyEnabled)) {
            isChecked = queryResult.get(isEnhanced4gLteModeSettingEnabled);
            if (isEnabled || isChecked) {
                isNonTtyOrTtyOnVolteEnabled =
                        queryResult.get(isVolteTtyEnabled) || !queryResult.get(isTtyEnabled);
            }
        } catch (Exception exception) {
            Log.w(TAG, "fail to get status for update. subId=" + mSubId, exception);
        }

        switchPreference.setEnabled(isNonTtyOrTtyOnVolteEnabled && isEnabled);
        switchPreference.setChecked(isNonTtyOrTtyOnVolteEnabled && isChecked);
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
        imsMmTelManager.setAdvancedCallingSettingEnabled(isChecked);
        for (final On4gLteUpdateListener lsn : m4gLteListeners) {
            lsn.on4gLteUpdated();
        }
        return true;
    }

    @Override
    public boolean isChecked() {
        final ImsQuery isEnhanced4gLteModeSettingEnabled =
                isEnhanced4gLteModeSettingEnabledByUser(mSubId);

        try (ImsQueryResult queryResult = new ImsQueryResult(
                isEnhanced4gLteModeSettingEnabled)) {
            return queryResult.get(isEnhanced4gLteModeSettingEnabled);
        } catch (Exception exception) {
            Log.w(TAG, "fail to get setting status. subId=" + mSubId, exception);
        }
        return false;
    }

    public Enhanced4gBasePreferenceController addListener(On4gLteUpdateListener lsn) {
        m4gLteListeners.add(lsn);
        return this;
    }

    protected int getMode() {
        return MODE_NONE;
    }

    private boolean isModeMatched() {
        return m4gCurrentMode == getMode();
    }

    private boolean isPrefEnabled() {
        return mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && (mCallState != null) && (mCallState == TelephonyManager.CALL_STATE_IDLE)
                && mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL);
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
    ImsQuery isImsServiceStateReady(int subId) {
        return new ImsQueryFeatureIsReady(subId);
    }

    @VisibleForTesting
    ImsQuery isVolteProvisionedOnDevice(int subId) {
        return new ImsQueryProvisioningStat(subId,
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                ImsRegistrationImplBase.REGISTRATION_TECH_LTE);
    }

    @VisibleForTesting
    ImsQuery isVolteEnabledByPlatform(int subId) {
        return new ImsQuerySupportStat(subId,
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
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
     * Update other preferences when 4gLte state is changed
     */
    public interface On4gLteUpdateListener {
        void on4gLteUpdated();
    }
}

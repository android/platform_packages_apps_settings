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

package com.android.settings;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.settings.network.GlobalSettingsChangeListener;
import com.android.settings.network.ProxySubscriptionManager;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitor and update configuration of airplane mode settings
 */
public class AirplaneModeEnabler extends GlobalSettingsChangeListener {

    private static final String LOG_TAG = "AirplaneModeEnabler";
    private static final boolean DEBUG = false;

    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private OnAirplaneModeChangedListener mOnAirplaneModeChangedListener;

    public interface OnAirplaneModeChangedListener {
        /**
         * Called when airplane mode status is changed.
         *
         * @param isAirplaneModeOn the airplane mode is on
         */
        void onAirplaneModeChanged(boolean isAirplaneModeOn);
    }

    private TelephonyManager mTelephonyManager;
    private ProxySubscriptionManager mProxySubscriptionMgr;
    private List<ServiceStateListener> mServiceStateListeners;

    private GlobalSettingsChangeListener mAirplaneModeObserver;

    public AirplaneModeEnabler(Context context,
            OnAirplaneModeChangedListener listener) {
        super(context, Settings.Global.AIRPLANE_MODE_ON);

        mContext = context;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mOnAirplaneModeChangedListener = listener;

        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mProxySubscriptionMgr = ProxySubscriptionManager.getInstance(context);
    }

    /**
     * Implementation of GlobalSettingsChangeListener.onChanged
     */
    public void onChanged(String field) {
        if (DEBUG) {
            Log.d(LOG_TAG, "Airplane mode configuration update");
        }
        onAirplaneModeChanged();
    }

    public void resume() {
        final List<SubscriptionInfo> subInfoList =
                mProxySubscriptionMgr.getActiveSubscriptionsInfo();

        mServiceStateListeners = new ArrayList<ServiceStateListener>();

        // add default listener
        mServiceStateListeners.add(new ServiceStateListener(mTelephonyManager,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID, this));

        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                mServiceStateListeners.add(new ServiceStateListener(mTelephonyManager,
                        subInfo.getSubscriptionId(), this));
            }
        }

        for (ServiceStateListener ssListener : mServiceStateListeners) {
            ssListener.start();
        }
    }

    public void pause() {
        if (mServiceStateListeners == null) {
            return;
        }
        for (ServiceStateListener ssListener : mServiceStateListeners) {
            ssListener.stop();
        }
        mServiceStateListeners = null;
    }

    private void setAirplaneModeOn(boolean enabling) {
        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                enabling ? 1 : 0);

        // Notify listener the system setting is changed.
        if (mOnAirplaneModeChangedListener != null) {
            mOnAirplaneModeChangedListener.onAirplaneModeChanged(enabling);
        }

        // Post the intent
        final Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     * TODO: We update the checkbox summary when we get notified
     * that mobile radio is powered up/down. We should not have dependency
     * on one radio alone. We need to do the following:
     * - handle the case of wifi/bluetooth failures
     * - mobile does not send failure notification, fail on timeout.
     */
    private void onAirplaneModeChanged() {
        if (mOnAirplaneModeChangedListener != null) {
            mOnAirplaneModeChangedListener.onAirplaneModeChanged(isAirplaneModeOn());
        }
    }

    /**
     * Check the status of ECM mode
     *
     * @return any subscription within device is under ECM mode
     */
    public boolean isInEcmMode() {
        if (mTelephonyManager.getEmergencyCallbackMode()) {
            return true;
        }
        final List<SubscriptionInfo> subInfoList =
                mProxySubscriptionMgr.getActiveSubscriptionsInfo();
        if (subInfoList == null) {
            return false;
        }
        for (SubscriptionInfo subInfo : subInfoList) {
            final TelephonyManager telephonyManager =
                    mTelephonyManager.createForSubscriptionId(subInfo.getSubscriptionId());
            if (telephonyManager != null) {
                if (telephonyManager.getEmergencyCallbackMode()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setAirplaneMode(boolean isAirplaneModeOn) {
        if (isInEcmMode()) {
            // In ECM mode, do not update database at this point
            Log.d(LOG_TAG, "ECM airplane mode=" + isAirplaneModeOn);
        } else {
            mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_AIRPLANE_TOGGLE,
                    isAirplaneModeOn);
            setAirplaneModeOn(isAirplaneModeOn);
        }
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        Log.d(LOG_TAG, "Exist ECM=" + isECMExit + ", with airplane mode=" + isAirplaneModeOn);
        if (isECMExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // update summary
            onAirplaneModeChanged();
        }
    }

    public boolean isAirplaneModeOn() {
        return WirelessUtils.isAirplaneModeOn(mContext);
    }

    private static class ServiceStateListener extends PhoneStateListener {
        private ServiceStateListener(TelephonyManager telephonyManager, int subscriptionId,
                AirplaneModeEnabler enabler) {
            super();
            mSubId = subscriptionId;
            mTelephonyManager = getSubIdSpecificTelephonyManager(telephonyManager);
            mEnabler = enabler;
        }

        private int mSubId;
        private TelephonyManager mTelephonyManager;
        private AirplaneModeEnabler mEnabler;

        int getSubscriptionId() {
            return mSubId;
        }

        void start() {
            if (mTelephonyManager != null) {
                mTelephonyManager.listen(this, PhoneStateListener.LISTEN_SERVICE_STATE);
            }
        }

        void stop() {
            if (mTelephonyManager != null) {
                mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
            }
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            if (DEBUG) {
                Log.d(LOG_TAG, "ServiceState in sub" + mSubId + ": " + serviceState);
            }
            mEnabler.onAirplaneModeChanged();
        }

        private TelephonyManager getSubIdSpecificTelephonyManager(
                TelephonyManager telephonyManager) {
            if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                return telephonyManager;
            }
            return telephonyManager.createForSubscriptionId(mSubId);
        }
    }

}

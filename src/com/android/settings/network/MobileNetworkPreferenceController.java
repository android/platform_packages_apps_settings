/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.network;

import android.content.Context;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.Utils;
import com.android.settings.core.PreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

import static android.os.UserHandle.myUserId;
import static android.os.UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS;

public class MobileNetworkPreferenceController extends PreferenceController implements
        LifecycleObserver, OnResume, OnPause {

    private static final String KEY_MOBILE_NETWORK_SETTINGS = "mobile_network_settings";

    private final boolean mIsSecondaryUser;
    private final TelephonyManager mTelephonyManager;
    private final UserManager mUserManager;
    private Preference mPreference;
    private SubscriptionManager mSubscriptionManager;

    @VisibleForTesting
    final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            updateDisplayName();
        }
    };

    @VisibleForTesting
    final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
             updateDisplayName();
        }
    };

    public MobileNetworkPreferenceController(Context context) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mIsSecondaryUser = !mUserManager.isAdminUser();
        mSubscriptionManager = SubscriptionManager.from(context);
    }

    @Override
    public boolean isAvailable() {
        return !isUserRestricted() && !Utils.isWifiOnly(mContext);
    }

    public boolean isUserRestricted() {
        final RestrictedLockUtilsWrapper wrapper = new RestrictedLockUtilsWrapper();
        return mIsSecondaryUser ||
                wrapper.hasBaseUserRestriction(
                        mContext,
                        DISALLOW_CONFIG_MOBILE_NETWORKS,
                        myUserId());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = screen.findPreference(getPreferenceKey());
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_MOBILE_NETWORK_SETTINGS;
    }

    @Override
    public void onResume() {
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        if (isAvailable()) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        }
    }

    private void updateDisplayName() {
        if (mPreference != null) {
            List<SubscriptionInfo> list = mSubscriptionManager.getActiveSubscriptionInfoList();
            if (list != null && !list.isEmpty()) {
                boolean useSeparator = false;
                StringBuilder builder = new StringBuilder();
                for (SubscriptionInfo subInfo : list) {
                    if (isSubscriptionInService(subInfo.getSubscriptionId())) {
                        if (useSeparator) builder.append(", ");
                        builder.append(mTelephonyManager.getNetworkOperatorName(
                                subInfo.getSubscriptionId()));
                        useSeparator = true;
                    }
                }
                mPreference.setSummary(builder.toString());
            } else {
                mPreference.setSummary(mTelephonyManager.getNetworkOperatorName());
            }
        }
    }

    @VisibleForTesting
    boolean isSubscriptionInService(int subId) {
        return (mTelephonyManager.getServiceStateForSubscriber(subId).getState()
                == ServiceState.STATE_IN_SERVICE);
    }

    @Override
    public void onPause() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mSubscriptionManager
                .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
    }
}

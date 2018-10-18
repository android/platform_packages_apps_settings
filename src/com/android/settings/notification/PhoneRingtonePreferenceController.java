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

package com.android.settings.notification;

import android.content.Context;
import android.media.RingtoneManager;

import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.DefaultRingtonePreference;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.List;

public class PhoneRingtonePreferenceController extends RingtonePreferenceControllerBase {

    private static final String KEY_PHONE_RINGTONE = "ringtone";
    private static final String EMERGENCY_PHONE_ACCOUNT_HANDLE_ID = "E";
    private static final String RINGTONE_DELIMITER_FOR_PHONE_ACCOUNT_HANDLE = " - ";
    private static final int PHONE_RINGTONE_PREFERENCE_ID = 0;

    private TelecomManager mTelecomManager;

    public PhoneRingtonePreferenceController(Context context) {
        super(context);
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        DefaultRingtonePreference ringtonePreference =
                (DefaultRingtonePreference) screen.findPreference(getPreferenceKey());
        PhoneAccountHandle phoneAccountHandle = getCurrentPhoneAccountHandle();
        ringtonePreference.setPhoneAccountHandle(phoneAccountHandle);

        if (hasMultiPhoneAccountHandle()) {
            // For multi PhoneAccountHandle case, will show multi ringtone setting.
            // So shoud show "Phone ringtone - operator name" for each ringtone setting.
            CharSequence displayName = getDisplayNameForRingtonePreference(phoneAccountHandle);
            if (displayName != null) {
                ringtonePreference.setTitle(mContext.getString(R.string.ringtone_title)
                        + RINGTONE_DELIMITER_FOR_PHONE_ACCOUNT_HANDLE + displayName);
            }
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PHONE_RINGTONE;
    }

    @Override
    public boolean isAvailable() {
        return Utils.isVoiceCapable(mContext);
    }

    @Override
    public int getRingtoneType() {
        return RingtoneManager.TYPE_RINGTONE;
    }

    public int getIdForPhoneRingtonePreference() {
        return PHONE_RINGTONE_PREFERENCE_ID;
    }

    public boolean hasMultiPhoneAccountHandle() {
        return getPhoneAccountHandles().size() > 1;
    }

    private PhoneAccountHandle getCurrentPhoneAccountHandle() {
        List<PhoneAccountHandle> accountHandles = getPhoneAccountHandles();
        int id = getIdForPhoneRingtonePreference();
        if (accountHandles != null && accountHandles.size() > id) {
            return accountHandles.get(id);
        }
        return null;
    }

    // This method is to get the available phone account handles of the SIMs.
    private List<PhoneAccountHandle> getPhoneAccountHandles() {
        List<PhoneAccountHandle> subscriptionAccountHandles = new ArrayList<>();
        List<PhoneAccountHandle> accountHandles = mTelecomManager.getCallCapablePhoneAccounts(true);
        for (PhoneAccountHandle accountHandle : accountHandles) {
            PhoneAccount phoneAccount = mTelecomManager.getPhoneAccount(accountHandle);
            // Emergency phone account also has CAPABILITY_SIM_SUBSCRIPTION, so should exclude it.
            if (phoneAccount.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)
                    && !EMERGENCY_PHONE_ACCOUNT_HANDLE_ID.equals(accountHandle.getId())) {
                subscriptionAccountHandles.add(accountHandle);
            }
        }
        return subscriptionAccountHandles;
    }

    private CharSequence getDisplayNameForRingtonePreference(
                PhoneAccountHandle phoneAccountHandle) {
        TelephonyManager telephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        SubscriptionManager subscriptionManager = (SubscriptionManager) mContext
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        PhoneAccount phoneAccount = mTelecomManager.getPhoneAccount(phoneAccountHandle);
        int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
        SubscriptionInfo subInfo = subscriptionManager.getActiveSubscriptionInfo(subId);
        if (subInfo != null) {
            return subInfo.getDisplayName();
        }
        return null;
    }
}

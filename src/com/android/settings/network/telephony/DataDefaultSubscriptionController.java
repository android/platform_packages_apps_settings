/*
 * Copyright (c) 2021, The Android Open Source Project
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
 import android.telephony.SubscriptionInfo;
 import android.telephony.SubscriptionManager;

public class DataDefaultSubscriptionController extends DefaultSubscriptionController {

    private static final String SETTING_USER_PREF_DATA_SUB = "user_preferred_data_sub";

    public DataDefaultSubscriptionController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected SubscriptionInfo getDefaultSubscriptionInfo() {
        return mManager.getActiveSubscriptionInfo(getDefaultSubscriptionId());
    }

    @Override
    protected int getDefaultSubscriptionId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    @Override
    protected void setDefaultSubscription(int subscriptionId) {
        mManager.setDefaultDataSubId(subscriptionId);
        setUserPrefDataSubIdInDb(subscriptionId);
    }

    private void setUserPrefDataSubIdInDb(int subId) {
        android.provider.Settings.Global.putInt(mContext.getContentResolver(),
                SETTING_USER_PREF_DATA_SUB, subId);
    }
}

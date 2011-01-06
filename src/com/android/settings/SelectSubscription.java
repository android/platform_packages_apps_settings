/*
 * Copyright (c) 2010, Code Aurora Forum. All rights reserved.
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

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;


public class SelectSubscription extends PreferenceActivity {

    private static final String KEY_SUBSCRIPTION_01 = "subscription_01";
    private static final String KEY_SUBSCRIPTION_02 = "subscription_02";
    public static final String SUBSCRIPTION_ID = "SUBSCRIPTION_ID";
    public static final String PACKAGE = "PACKAGE";
    public static final String TARGET_CLASS = "TARGET_CLASS";

    private PreferenceScreen subscriptionPref0, subscriptionPref1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.select_subscription);
    }

    @Override
    protected void onResume() {
        super.onResume();

        subscriptionPref0 = (PreferenceScreen) findPreference(KEY_SUBSCRIPTION_01);
        subscriptionPref1 = (PreferenceScreen) findPreference(KEY_SUBSCRIPTION_02);

        Intent intent =  getIntent();
        String pkg = intent.getStringExtra(PACKAGE);
        String targetClass = intent.getStringExtra(TARGET_CLASS);
        // Set the target class.
        // suscription_01 denotes subscription id 0
        // suscription_02 denotes subscription id 1   
        subscriptionPref0.getIntent().setClassName(pkg, targetClass);
        subscriptionPref1.getIntent().setClassName(pkg, targetClass);
        subscriptionPref0.getIntent().putExtra(SUBSCRIPTION_ID, 0);
        subscriptionPref1.getIntent().putExtra(SUBSCRIPTION_ID, 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}

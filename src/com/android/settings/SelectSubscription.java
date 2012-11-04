/* Copyright (c) 2010-12, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Code Aurora Forum, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;


public class SelectSubscription extends PreferenceActivity {

    private static final String KEY_SUBSCRIPTION_01 = "subscription_01";
    private static final String KEY_SUBSCRIPTION_02 = "subscription_02";
    public static final String SUBSCRIPTION_KEY = "subscription";
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
        subscriptionPref0.getIntent().putExtra(SUBSCRIPTION_KEY, 0);
        subscriptionPref1.getIntent().putExtra(SUBSCRIPTION_KEY, 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}

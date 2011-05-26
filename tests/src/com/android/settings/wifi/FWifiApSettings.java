/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.wifi;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import java.lang.reflect.Field;

public class FWifiApSettings extends ActivityInstrumentationTestCase2<WifiApSettings> {

    public FWifiApSettings() {
        super("com.android.settings", WifiApSettings.class);
    }

    private Instrumentation mInstrumentation;
    private WifiApSettings mActivity;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mInstrumentation.waitForIdleSync();
    }

    /**
     * Test when not configured that the WifiApDialog is opened immediately and
     * with a visible password field since by default WPA_PSK protection shall
     * be used.
     */
    public void testNotConfigured() {
        WifiApDialog wifiApDialog = (WifiApDialog) getMember(mActivity, "mDialog");
        assertNotNull("Dialog is not shown despite not configured", wifiApDialog);
        EditText password = (EditText) getMember(wifiApDialog, "mPassword");
        assertTrue("Member mPassword not shown", password.isShown());
    }

    /**
     * Gets a private member.
     *
     * @param instance Instance of the object from which to get the private member
     * @param member The name of the private member
     * @return the member object
     */
    private static Object getMember(Object instance, String member) {
        try {
            Class clazz = instance.getClass();
            Field field = clazz.getDeclaredField(member);
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            fail("getMember failed " + e);
        }
        return null;
    }
}

/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.tests.perf;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static junit.framework.TestCase.fail;

import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.view.KeyEvent;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(AndroidJUnit4.class)
public class LaunchSettingsTest {

    private static final int TIME_OUT = 5000;
    private static final int TEST_TIME = 10;
    private static final int WAIT_TIME = 60000;
    private static final Pattern PATTERN = Pattern.compile("TotalTime:\\s[0-9]*");
    private static final String[] ACTIVITIES =
            {
                    "android.settings.SETTINGS",
                    "android.settings.WIFI_SETTINGS",
                    "android.settings.BLUETOOTH_SETTINGS",
                    "android.settings.APPLICATION_SETTINGS",
                    "android.intent.action.POWER_USAGE_SUMMARY",
                    "android.settings.INTERNAL_STORAGE_SETTINGS"
            };
    private static final String[] PAGES =
            {"Settings", "Wi-Fi", "BlueTooth", "Application", "Battery", "Storage"};
    private static final String[] TEXTS =
            {"Search settings", "Use Wi‑Fi", "Connected devices", "App info", "Battery", "Storage"};
    private Bundle mBundle;
    private UiDevice mDevice;
    private Instrumentation mInstrumentation;
    private Map<String, ArrayList<Integer>> mResult;

    @Before
    public void setUp() throws Exception {
        mBundle = new Bundle();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mResult = new LinkedHashMap<>();
        mDevice.pressHome();
        Thread.sleep(WAIT_TIME);

        for (String string : PAGES) {
            mResult.put(string, new ArrayList<Integer>());
        }
    }

    @After
    public void tearDown() throws Exception {
        putResultToBundle();
        mInstrumentation.sendStatus(0, mBundle);
    }

    @Test
    public void settingsPerformanceTest() throws Exception {
        for (int i = 0; i < TEST_TIME; i++) {
            for (int j = 0; j < ACTIVITIES.length; j++) {
                executePreformanceTest(ACTIVITIES[j], TEXTS[j], j);
            }
        }

    }

    private void executePreformanceTest(String activity, String text, int page) throws Exception {
        final String mString = mDevice.executeShellCommand("am start -W -a" + activity);
        mDevice.wait(Until.findObject(By.text(text)), TIME_OUT);
        handleLaunchResult(page, mString);
        closeApp();
        mDevice.waitForIdle(TIME_OUT);
    }

    private void handleLaunchResult(int page, String s) {
        Matcher mMatcher = PATTERN.matcher(s);
        if (mMatcher.find()) {
            mResult.get(PAGES[page]).add(Integer.valueOf(mMatcher.group().split("\\s")[1]));
        } else {
            fail("Some pages can't be found");
        }
    }

    private void closeApp() throws Exception {
        mDevice.executeShellCommand("am force-stop com.android.settings");
        Thread.sleep(1000);
    }

    private void putResultToBundle() {
        for (String string : mResult.keySet()) {
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "max"),
                    getMax(mResult.get(string)));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "min"),
                    getMin(mResult.get(string)));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "avg"),
                    getAvg(mResult.get(string)));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "25 Percentile"),
                    getPercentile(mResult.get(string), 25));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "50 Percentile"),
                    getPercentile(mResult.get(string), 50));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "75 Percentile"),
                    getPercentile(mResult.get(string), 75));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "all_results"),
                    mResult.get(string).toString());
        }
    }

    private String getMax(ArrayList<Integer> launchResult) {
        return String.format("%s", launchResult.isEmpty() ? "null" : Collections.max(launchResult));
    }

    private String getMin(ArrayList<Integer> launchResult) {
        return String.format("%s", launchResult.isEmpty() ? "null" : Collections.min(launchResult));
    }

    private String getAvg(ArrayList<Integer> launchResult) {
        return String.valueOf((int) launchResult.stream().mapToInt(i -> i).average().orElse(0));
    }

    private String getPercentile(ArrayList<Integer> launchResult, double position) {
        Collections.sort(launchResult);
        return String.valueOf(launchResult.get((int) (Math.ceil(TEST_TIME * position / 100)) - 1));
    }
}
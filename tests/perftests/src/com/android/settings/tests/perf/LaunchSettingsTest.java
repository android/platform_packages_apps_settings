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

import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.fail;

@RunWith(AndroidJUnit4.class)
public class LaunchSettingsTest {
    private UiDevice mDevice;
    private final int LAUNCH_TIMEOUT = 5000;
    private final int TEST_TIME = 1;
    private final String[] Pages = {"Settings", "Network", "BlueTooth", "Application", "Battery"};
    private Map<String, ArrayList<Integer>> map = new LinkedHashMap<>();
    private Pattern pattern = Pattern.compile("TotalTime:\\s[0-9]*");
    private Matcher matcher = null;
    private String s = "";

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testReportMetrics() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final Bundle result = new Bundle();
        result.putString("LaunchSettingsTest_metric_key1", "1000");
        result.putString("LaunchSettingsTest_metric_key2", "5000");
        instrumentation.sendStatus(0, result);
    }

	@Test
    public void SettingsPerformanceTest() throws Exception{
        mDevice = UiDevice.getInstance(getInstrumentation());

        if(mDevice != null && TEST_TIME != 0) {
            // Back to home
            mDevice.pressHome();
            mDevice.waitForIdle(LAUNCH_TIMEOUT);

            for (String string : Pages){
                map.put(string, new ArrayList<Integer>());
            }

            for(int i = 0; i < TEST_TIME; i++){
                //Launch then close Setting page.
                s = mDevice.executeShellCommand("am start -W -S -n com.android.settings/.Settings");
                //mUiAutomation.executeShellCommand("am start -W -S -n com.android.settings/.Settings");
                mDevice.wait(Until.findObject(By.res("com.android.settings:id/search_action_bar")), LAUNCH_TIMEOUT);
                mDevice.executeShellCommand("pm clear com.android.settings");
                stringHandling(0, s);
                Thread.sleep(1000);

                //Launch then close NetWorkSetting page.
                s = mDevice.executeShellCommand("am start -W -a android.settings.NETWORK_OPERATOR_SETTINGS");
                mDevice.wait(Until.findObject(By.text("Mobile data")), LAUNCH_TIMEOUT);
                mDevice.executeShellCommand("pm clear com.android.settings");
                stringHandling(1, s);
                Thread.sleep(1000);

                //Launch then close Bluetooth page.
                s = mDevice.executeShellCommand("am start -W -a android.settings.BLUETOOTH_SETTINGS");
                mDevice.wait(Until.findObject(By.text("Connected devices")), LAUNCH_TIMEOUT);
                mDevice.executeShellCommand("pm clear com.android.settings");
                stringHandling(2, s);
                Thread.sleep(1000);

                //Launch then close App info page.
                s = mDevice.executeShellCommand("am start -W -a android.settings.APPLICATION_SETTINGS");
                mDevice.wait(Until.findObject(By.text("App info")), LAUNCH_TIMEOUT);
                mDevice.executeShellCommand("pm clear com.android.settings");
                stringHandling(3, s);
                Thread.sleep(1000);

                //Launch then close Battery usage page.
                s = mDevice.executeShellCommand("am start -W -a android.intent.action.POWER_USAGE_SUMMARY");
                mDevice.wait(Until.findObject(By.text("Battery")), LAUNCH_TIMEOUT);
                mDevice.executeShellCommand("pm clear com.android.settings");
                stringHandling(4, s);
                Thread.sleep(1000);
                }
            }

        else {
            fail("Something get wrong.");
            return;
        }

        for (String string: map.keySet()){
            Log.i("Pages",String.format("%s : %s", string, map.get(string)));
        }
    }

    public int stringHandling(int page ,String s){
        if(s == ""){
            return 0;
        }

        matcher = pattern.matcher(s);
        if (matcher.find()){
            map.get(Pages[page]).add(Integer.valueOf(matcher.group().split("\\s")[1]));
        }
        else {
            fail("Some page can't find");
        }

        return 1;
    }
}


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
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import android.support.test.uiautomator.UiSelector;

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

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.fail;

@RunWith(AndroidJUnit4.class)
public class LaunchSettingsTest {

  private final int TIME_OUT = 5000;
  private final int TEST_TIME = 10;
  private final Bundle RESULT = new Bundle();
  private final String[] Pages = {"Settings", "Wi-Fi", "BlueTooth", "Application", "Battery"};
  private Map<String, ArrayList<Integer>> map = new LinkedHashMap<>();
  private Pattern pattern = Pattern.compile("TotalTime:\\s[0-9]*");
  private Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
  private Matcher matcher = null;
  private UiDevice mDevice;

  private String mString = "";

  @Before
  public void setUp() throws Exception {
    mDevice = UiDevice.getInstance(getInstrumentation());
    Log.i("Pages", "Testing Start !!");
    Log.i("Pages", String.format("Run %s times !", TEST_TIME));
    mDevice.pressHome();
    mDevice.waitForIdle(TIME_OUT);

    for (String string : Pages) {
      map.put(string, new ArrayList<Integer>());
    }
  }

  @After
  public void tearDown() throws Exception {
    printResult();
    mInstrumentation.sendStatus(0, RESULT);
  }

//  @Test
//  public void testReportMetrics() throws Exception {
//    RESULT.putString("LaunchSettingsTest_metric_key1", "1000");
//    RESULT.putString("LaunchSettingsTest_metric_key2", "5000");
//  }

  @Test
  public void settingsPerformanceTest() throws Exception {
    for (int i = 0; i < TEST_TIME; i++) {
      launchPages("android.settings.SETTINGS", "Search settings", 0);
      launchPages("android.settings.WIFI_SETTINGS", "Use Wi‑Fi", 1);
      launchPages("android.settings.BLUETOOTH_SETTINGS", "Connected devices", 2);
      launchPages("android.settings.APPLICATION_SETTINGS", "App info", 3);
      launchPages("android.intent.action.POWER_USAGE_SUMMARY", "Battery", 4);
    }

  }

  private void launchPages(String activity, String text, int page) throws Exception{
    mString = mDevice.executeShellCommand("am start -W -a" + activity);
    mDevice.wait(Until.findObject(By.text(text)), TIME_OUT);
    stringHandling(page, mString);
    closeApp();
    mDevice.waitForIdle(TIME_OUT);
  }

  private int stringHandling(int page, String s) {
    if (s == "") {
      return 0;
    }

    matcher = pattern.matcher(s);
    if (matcher.find()) {
      map.get(Pages[page]).add(Integer.valueOf(matcher.group().split("\\s")[1]));
      //map.get(Pages[page]).add(matcher.group().split("\\s")[1]);
    } else {
      fail("Some pages can't be found");
    }

    return 1;
  }

  private void closeApp() throws Exception {
    mDevice.pressRecentApps();
    mDevice.findObject(new UiSelector().resourceId("com.android.launcher3:id/snapshot"))
        .swipeUp(10);
  }

  private void printResult() {
    for (String string : map.keySet()) {
      Log.i("Pages", String.format("%s max : %s", string, getMax(map.get(string))));
      Log.i("Pages", String.format("%s min : %s", string, getMin(map.get(string))));
      Log.i("Pages", String.format("%s avg : %s", string, getAvg(map.get(string))));
      RESULT.putString(String.format("LaunchSettingsTest_%s_%s", string, "max"),
          getMax(map.get(string)));
      RESULT.putString(String.format("LaunchSettingsTest_%s_%s", string, "min"),
          getMin(map.get(string)));
      RESULT.putString(String.format("LaunchSettingsTest_%s_%s", string, "avg"),
          getAvg(map.get(string)));
    }
  }

  private String getMax(ArrayList<Integer> al){
    return String.format("%s", al.isEmpty() ? "null" : Collections.max(al));
  }

  private String getMin(ArrayList<Integer> al){
    return String.format("%s", al.isEmpty() ? "null" : Collections.min(al));
  }

  private String getAvg(ArrayList<Integer> al) {
    if (al.size() == 0) return "0";
    int sum = 0;
    for (int i : al) {
      sum += i;
    }
    return String.valueOf(sum / al.size());
  }
}

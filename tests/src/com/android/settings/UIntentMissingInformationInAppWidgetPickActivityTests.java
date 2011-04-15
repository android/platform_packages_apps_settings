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

package com.android.settings;

import android.content.Intent;
import android.test.ActivityUnitTestCase;

public class UIntentMissingInformationInAppWidgetPickActivityTests extends
        ActivityUnitTestCase<AppWidgetPickActivity> {

    public UIntentMissingInformationInAppWidgetPickActivityTests() {
        super(AppWidgetPickActivity.class);
    }

    public UIntentMissingInformationInAppWidgetPickActivityTests(
            Class<AppWidgetPickActivity> activityClass) {
        super(activityClass);
    }

    /**
     * Test sending an Intent missing extras information to
     * AppWidgetPickActivity.
     */
    public void testIntentToAppWidgetPickActivity() throws Exception {
        AppWidgetPickActivity testedActivity = null;
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "AppWidgetPickActivity");

        try {
            testedActivity = (AppWidgetPickActivity)startActivity(intent, null, null);
        } catch (Exception e) {
            fail("UIntentMissingInformationInAppWidgetPickActivityTests: " + e);
        }
    }
}

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

import com.android.settings.ApplicationSettings;
import android.test.ActivityInstrumentationTestCase2;

import android.content.res.Configuration;
import android.preference.Preference;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

/**
 * Tests related to the quick launch menu item in the application menu.
 */
public class QuickLaunchTests extends ActivityInstrumentationTestCase2<ApplicationSettings> {

    private static final String KEY_QUICK_LAUNCH = "quick_launch";

    ApplicationSettings mApplicationSettings;

    public QuickLaunchTests() {
        super(ApplicationSettings.class.getPackage().getName(), ApplicationSettings.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mApplicationSettings = getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mApplicationSettings = null;
    }

    /**
     * Test that the QuickLaunch menu is only available if there
     * is both a HW keyboard and a HW search button.
     */
    public void testQuickLaunchMenuAvailability() {
        Preference quickLaunchSetting = mApplicationSettings.findPreference(KEY_QUICK_LAUNCH);

        if (mApplicationSettings.getResources().getConfiguration().keyboard ==
                    Configuration.KEYBOARD_NOKEYS
                    || !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_SEARCH)) {
            // If HW keyboard or search key is missing, the setting for
            // quick launch should not be available
            assertNull("QuickLaunch menu item is available even though it should not be",
                    quickLaunchSetting);
        } else {
            assertNotNull("QuickLaunch menu item is not available even though it should be",
                    quickLaunchSetting);
        }
    }
}

/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WifiDppEnrolleeActivityTest {
    @Rule
    public final ActivityTestRule<WifiDppEnrolleeActivity> mActivityRule =
            new ActivityTestRule<>(WifiDppEnrolleeActivity.class);

    @Test
    public void testActivity_shouldImplementsOnScanWifiDppSuccessCallback() {
        WifiDppEnrolleeActivity activity = mActivityRule.getActivity();

        assertThat(activity instanceof WifiDppQrCodeScannerFragment
                .OnScanWifiDppSuccessListener).isEqualTo(true);
    }
}

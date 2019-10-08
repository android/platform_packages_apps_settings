/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.assist;

import static com.google.common.truth.Truth.assertThat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ManageAssistTest {

    private ManageAssist mSettings;

    @Before
    public void setUp() {
        mSettings = new ManageAssist();
    }

    @Test
    public void testGetMetricsCategory() {
        assertThat(mSettings.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.APPLICATIONS_MANAGE_ASSIST);
    }

    @Test
    public void testGetCategoryKey() {
        assertThat(mSettings.getCategoryKey()).isNull();
    }

    @Test
    public void testGetPreferenceScreenResId() {
        assertThat(mSettings.getPreferenceScreenResId()).isEqualTo(R.xml.manage_assist);
    }
}

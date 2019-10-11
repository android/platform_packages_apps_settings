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
 * limitations under the License
 */

package com.android.settings.applications.appinfo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.view.WindowManager.LayoutParams;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DrawOverlayDetailsTest {

    private LayoutParams layoutParams;

    private FakeFeatureFactory mFeatureFactory;

    @Spy
    private DrawOverlayDetails mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        layoutParams = new LayoutParams();
    }

    @Test
    public void logSpecialPermissionChange() {
        when(mFragment.getContext()).thenReturn(RuntimeEnvironment.application);

        mFragment.logSpecialPermissionChange(true, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_APPDRAW_ALLOW,
                mFragment.getMetricsCategory(), "app", 0);

        mFragment.logSpecialPermissionChange(false, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_APPDRAW_DENY,
                mFragment.getMetricsCategory(), "app", 0);
    }
}

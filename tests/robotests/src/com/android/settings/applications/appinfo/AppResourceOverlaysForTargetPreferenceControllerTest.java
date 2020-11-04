/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsActivity;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppResourceOverlaysForTargetPreferenceControllerTest {

    @Mock
    private SettingsActivity mActivity;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private ApplicationInfo mAppInfo;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;
    @Mock
    private ApplicationsState mState;

    private Context mContext;
    private AppResourceOverlaysForTargetPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.applicationInfo = mAppInfo;
        packageInfo.overlayTarget = "test.package";
        when(mFragment.getPackageInfo()).thenReturn(packageInfo);

        mController = new AppResourceOverlaysForTargetPreferenceController(mContext);
        mController.setParent(mFragment);
        when(mScreen.findPreference(any())).thenReturn(mPreference);
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
        when(mFragment.getActivity()).thenReturn(mActivity);
    }

    @Test
    public void getAvailabilityStatus_isAlwaysAvailable() {
        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(AppResourceOverlaysForTargetPreferenceController.AVAILABLE);
    }

    @Test
    public void displayPreference_isNotRro_shouldNotShowPreference() {
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = mock(ApplicationInfo.class);
        when(mFragment.getAppEntry()).thenReturn(appEntry);
        when(appEntry.info.isResourceOverlay()).thenReturn(false);

        mController.displayPreference(mScreen);

        verify(mPreference).setVisible(false);
    }

    @Test
    public void displayPreference_isRro_shouldShowPreference() {
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = mock(ApplicationInfo.class);
        when(mFragment.getAppEntry()).thenReturn(appEntry);
        when(appEntry.info.isResourceOverlay()).thenReturn(true);

        mController.displayPreference(mScreen);

        verify(mPreference).setVisible(true);
    }

    @Test
    public void updateState_shouldUpdatePreferenceSummary() {
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = new ApplicationInfo();
        appEntry.info.flags |= ApplicationInfo.PRIVATE_FLAG_IS_RESOURCE_OVERLAY;
        when(mFragment.getAppEntry()).thenReturn(appEntry);

        final AppEntry targetEntry = mock(AppEntry.class);
        targetEntry.label = "testpackage";
        when(mState.getEntry(anyString(), anyInt())).thenReturn(targetEntry);
        verify(mPreference, never()).setSummary("testpackage");
    }
}

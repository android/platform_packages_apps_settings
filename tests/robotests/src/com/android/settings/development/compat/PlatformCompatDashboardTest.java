/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.compat;

import static com.google.common.truth.Truth.assertThat;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyInt;

import android.compat.Compatibility.ChangeConfig;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.RemoteException;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.Arrays;
import java.util.HashSet;

import com.android.internal.compat.IPlatformCompat;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.internal.compat.CompatibilityChangeConfig;
import com.android.internal.compat.CompatibilityChangeInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PlatformCompatDashboardTest {
    private PlatformCompatDashboard mDashboard;

    @Mock
    private IPlatformCompat mPlatformCompat;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private ApplicationInfo mApplicationInfo;

    private Context mContext;
    private CompatibilityChangeInfo[] mChanges;
    private static final String APP_NAME = "foo.bar.baz";

    @Before
    public void setUp() throws RemoteException, NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mDashboard = new PlatformCompatDashboard() {
            @Override
            protected PackageManager getPackageManager() {
                return mPackageManager;
            }
            @Override
            IPlatformCompat getPlatformCompat() {
                return mPlatformCompat;
            }
            @Override
            String getSelectedApp() {
                return APP_NAME;
            }
            @Override
            public PreferenceScreen getPreferenceScreen() {
                return mPreferenceScreen;
            }
        };
        mChanges = new CompatibilityChangeInfo[5];
        mChanges[0] = new CompatibilityChangeInfo(1L, "Default_Enabled", 0, false);
        mChanges[1] = new CompatibilityChangeInfo(2L, "Default_Disabled", 0, true);
        mChanges[2] = new CompatibilityChangeInfo(3L, "Enabled_After_SDK_1_1", 1, false);
        mChanges[3] = new CompatibilityChangeInfo(4L, "Enabled_After_SDK_1_2", 1, false);
        mChanges[4] = new CompatibilityChangeInfo(5L, "Enabled_After_SDK_2", 2, false);
        when(mPlatformCompat.listAllChanges()).thenReturn(mChanges);
        mContext = RuntimeEnvironment.application;
        when(mPreferenceScreen.getContext()).thenReturn(mContext);
        mApplicationInfo.packageName = APP_NAME;
        when(mPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(mApplicationInfo);
    }

    @Test
    public void shouldNotHaveHelpResource() {
        assertThat(mDashboard.getHelpResource()).isEqualTo(0);
    }

    @Test
    public void shouldLogAsPlatformCompatPage() {
        assertThat(mDashboard.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.SETTINGS_PLATFORM_COMPAT_DASHBOARD);
    }

    @Test
    public void shouldUsePlatformCompatPreferenceLayout() {
        assertThat(mDashboard.getPreferenceScreenResId())
                .isEqualTo(R.xml.platform_compat_settings);
    }

    @Test
    public void testCreateAppPreference() {
        mApplicationInfo.targetSdkVersion = 1;
        Preference appPreference = mDashboard.createAppPreference();
        assertThat(appPreference.getSummary()).isEqualTo(APP_NAME + " SDK 1");
    }

    @Test
    public void createPreferenceForChange() {
        CompatibilityChangeInfo change = mChanges[0];
        CompatibilityChangeConfig config = new CompatibilityChangeConfig(
            new ChangeConfig(new HashSet<Long>(Arrays.asList(change.getId())),
                             new HashSet<Long>()));
        Preference preference = mDashboard.createPreferenceForChange(mContext, change, config);
    }
}

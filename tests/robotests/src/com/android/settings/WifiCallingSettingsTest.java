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

package com.android.settings;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.PersistableBundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;

import com.android.ims.ImsManager;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WifiCallingSettingsTest {
    private static final String TEST_EMERGENCY_ADDRESS_CARRIER_APP =
            "com.android.settings/.TestEmergencyAddressCarrierApp";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private CarrierConfigManager mMockCarrierConfigManager;
    @Mock
    private PreferenceScreen mMockPreferenceScreen;
    @Mock
    private ListPreference mMockButtonWfcMode;
    @Mock
    private ListPreference mMockButtonWfcRoamingMode;
    @Mock
    private Preference mMockUpdateAddress;

    private final PersistableBundle mBundle = new PersistableBundle();
    private WifiCallingSettings mFragment;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest(mActivity);
        mFragment = spy(new WifiCallingSettings());

        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mMockPreferenceScreen).when(mFragment).getPreferenceScreen();
        doReturn(mMockCarrierConfigManager).when(mActivity).getSystemService(
                CarrierConfigManager.class);
        doNothing().when(mFragment).startActivityForResult(any(Intent.class), anyInt());

        when(mMockCarrierConfigManager.getConfig()).thenReturn(mBundle);
        mBundle.putString(
                CarrierConfigManager.KEY_WFC_EMERGENCY_ADDRESS_CARRIER_APP_STRING,
                TEST_EMERGENCY_ADDRESS_CARRIER_APP);

        ReflectionHelpers.setField(mFragment, "mButtonWfcMode", mMockButtonWfcMode);
        ReflectionHelpers.setField(mFragment, "mButtonWfcRoamingMode", mMockButtonWfcRoamingMode);
        ReflectionHelpers.setField(mFragment, "mUpdateAddress", mMockUpdateAddress);
        ReflectionHelpers.setField(mFragment, "mMetricsFeatureProvider",
                mFeatureFactory.metricsFeatureProvider);
        ReflectionHelpers.setField(mFragment, "mEditableWfcMode", true);
        ReflectionHelpers.setField(mFragment, "mEditableWfcRoamingMode", true);
    }

    @Test
    public void onSwitchChanged_enableSetting_shouldLaunchWfcDisclaimerActivity() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        mFragment.onSwitchChanged(null, true);

        // Check the WFC disclaimer activity is launched.
        verify(mFragment).startActivityForResult(intentCaptor.capture(),
                eq(WifiCallingSettings.REQUEST_CHECK_WFC_DISCLAIMER));
        Intent intent = intentCaptor.getValue();
        assertEquals(intent.getComponent(), ComponentName.unflattenFromString(
                WifiCallingSettings.DISCLAIMER_ACTIVITY_STRING));
    }

    @Test
    public void onSwitchChanged_disableSetting_shouldNotLaunchWfcDisclaimerActivity() {
        mFragment.onSwitchChanged(null, false);

        // Check the WFC disclaimer activity is not launched.
        verify(mFragment, never()).startActivityForResult(any(Intent.class), anyInt());
    }

    @Test
    public void onActivityResult_finishWfcDisclaimerActivity_shouldLaunchCarrierActivity() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        // Emulate the WfcDisclaimerActivity finish.
        mFragment.onActivityResult(WifiCallingSettings.REQUEST_CHECK_WFC_DISCLAIMER,
                Activity.RESULT_OK, null);

        // Check the WFC emergency address activity is launched.
        verify(mFragment).startActivityForResult(intentCaptor.capture(),
                eq(WifiCallingSettings.REQUEST_CHECK_WFC_EMERGENCY_ADDRESS));
        Intent intent = intentCaptor.getValue();
        assertEquals(intent.getComponent(), ComponentName.unflattenFromString(
                TEST_EMERGENCY_ADDRESS_CARRIER_APP));
    }

    @Test
    public void onActivityResult_finishCarrierActivity_shouldShowWfcPreference() {
        mFragment.onActivityResult(WifiCallingSettings.REQUEST_CHECK_WFC_EMERGENCY_ADDRESS,
                Activity.RESULT_OK, null);

        // Check the WFC preferences is added.
        verify(mMockPreferenceScreen).addPreference(mMockButtonWfcMode);
        verify(mMockPreferenceScreen).addPreference(mMockButtonWfcRoamingMode);
        verify(mMockPreferenceScreen).addPreference(mMockUpdateAddress);
        // Check the WFC is enabled.
        assertTrue(ImsManager.isWfcEnabledByUser(null));
    }
}

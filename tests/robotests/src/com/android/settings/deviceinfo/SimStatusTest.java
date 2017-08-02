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

package com.android.settings.deviceinfo;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SimStatusTest {
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private Phone mPhone;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private PreferenceScreen mMockScreen;
    @Mock
    private Preference mMockImsRegistrationStatePreference;

    private Context mContext;
    private Resources mResources;
    private PersistableBundle mBundle;
    private SimStatus mFragment;

    private String mImsRegSummaryText;
    private boolean mImsRegRemoved;
    private boolean mResourceUpdated;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mResources = mContext.getResources();
        mBundle = new PersistableBundle();
        mFragment = spy(new SimStatus());

        doReturn(mContext).when(mFragment).getContext();
        doReturn(mMockScreen).when(mFragment).getPreferenceScreen();
        doReturn(mMockImsRegistrationStatePreference).when(mFragment).findPreference(
                SimStatus.KEY_IMS_REGISTRATION_STATE);
        doNothing().when(mFragment).addPreferencesFromResource(anyInt());

        ReflectionHelpers.setField(mFragment, "mCarrierConfigManager", mCarrierConfigManager);
        ReflectionHelpers.setField(mFragment, "mPhone", mPhone);
        ReflectionHelpers.setField(mFragment, "mRes", mResources);
        ReflectionHelpers.setField(mFragment, "mSir", mSubscriptionInfo);
        ReflectionHelpers.setField(mFragment, "mTelephonyManager", mTelephonyManager);

        when(mSubscriptionInfo.getSubscriptionId()).thenReturn(0);
        when(mCarrierConfigManager.getConfigForSubId(anyInt())).thenReturn(mBundle);
    }

    @Test
    public void testImsRegistrationStatusSummaryText() {
        when(mTelephonyManager.isImsRegistered(anyInt())).thenReturn(true);
        mFragment.updateImsRegistrationState();
        // Check "Registered" is set in the summary text
        verify(mMockImsRegistrationStatePreference, atLeastOnce()).setSummary(mContext.getString(
                R.string.ims_reg_status_registered));

        when(mTelephonyManager.isImsRegistered(anyInt())).thenReturn(false);
        mFragment.updateImsRegistrationState();
        // Check "Not registered" is set in the summary text
        verify(mMockImsRegistrationStatePreference, atLeastOnce()).setSummary(mContext.getString(
                R.string.ims_reg_status_not_registered));
    }

    @Test
    public void testRemoveImsRegistrationStatePreference() {
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, true);
        mFragment.updatePreference(false);
        // Check the preference is not removed if the config is true
        verify(mMockScreen, never()).removePreference(mMockImsRegistrationStatePreference);

        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, false);
        mFragment.updatePreference(false);
        // Check the preference is removed if the config is false
        verify(mMockScreen, atLeastOnce()).removePreference(mMockImsRegistrationStatePreference);
    }

    @Test
    public void testUpdatePreference() {
        mFragment.updatePreference(true);
        // Check all preferences are removed once and added again
        verify(mMockScreen, atLeastOnce()).removeAll();
        verify(mFragment, atLeastOnce()).addPreferencesFromResource(R.xml.device_info_sim_status);
    }
}

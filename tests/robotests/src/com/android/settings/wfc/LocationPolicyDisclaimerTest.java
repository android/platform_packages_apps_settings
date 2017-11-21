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

package com.android.settings.wfc;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

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

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class LocationPolicyDisclaimerTest {
    private static final int TEST_SUB_ID = 0;

    @Mock
    LayoutInflater mMockInflater;
    @Mock
    TextView mMockView;
    @Mock
    TextView mMockTextView;
    @Mock
    CarrierConfigManager mMockCarrierConfigManager;

    private final PersistableBundle mBundle = new PersistableBundle();
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        doReturn(mMockInflater).when(mContext).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        doReturn(mMockCarrierConfigManager).when(mContext).getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        doReturn(getSharedPreferences()).when(mContext).getSharedPreferences(anyString(), anyInt());

        when(mMockInflater.inflate(anyInt(), anyObject(), anyBoolean())).thenReturn(mMockView);
        when(mMockView.findViewById(R.id.disclaimer_desc)).thenReturn(mMockTextView);
        when(mMockCarrierConfigManager.getConfigForSubId(anyInt())).thenReturn(mBundle);
    }

    @Test
    public void constructor_shouldCreateLocationPolicyDisclaimerView() {
        LocationPolicyDisclaimer disclaimerItem = new LocationPolicyDisclaimer(mContext, 0);

        // Check the WFC disclaimer item view is created.
        verify(mMockTextView).setText(R.string.wfc_disclaimer_location_desc_txt);
        assertEquals(disclaimerItem.getView(), mMockView);
    }

    @Test
    public void sholdShow_configTrue_shouldShowLocationPolicyDisclaimer() {
        LocationPolicyDisclaimer disclaimerItem = spy(new LocationPolicyDisclaimer(mContext, 0));
        doReturn(TEST_SUB_ID).when(disclaimerItem).getSubscriptionId();
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL, false);
        getSharedPreferences().edit().putBoolean(
                LocationPolicyDisclaimer.KEY_HAS_AGREED_LOCATION_DISCLAIMER + TEST_SUB_ID,
                false).commit();

        boolean result = disclaimerItem.shouldShow();

        // Check the WFC disclaimer item is should be shown.
        assertTrue(result);
    }

    @Test
    public void sholdShow_configFalse_shouldNotShowLocationPolicyDisclaimer() {
        LocationPolicyDisclaimer disclaimerItem = new LocationPolicyDisclaimer(mContext, 0);
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL, false);

        boolean result = disclaimerItem.shouldShow();

        // Check the WFC disclaimer item is should not be shown due to the
        // KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL on carrier config is false.
        assertFalse(result);
    }

    @Test
    public void sholdShow_defaultWfcEnabled_shouldNotShowLocationPolicyDisclaimer() {
        LocationPolicyDisclaimer disclaimerItem = new LocationPolicyDisclaimer(mContext, 0);
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL, true);

        boolean result = disclaimerItem.shouldShow();

        // Check the WFC disclaimer item is should not be shown due to the
        // KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL on carrier config is true.
        assertFalse(result);
    }

    @Test
    public void sholdShow_alreadyAgreed_shouldNotShowLocationPolicyDisclaimer() {
        LocationPolicyDisclaimer disclaimerItem = spy(new LocationPolicyDisclaimer(mContext, 0));
        doReturn(TEST_SUB_ID).when(disclaimerItem).getSubscriptionId();
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL, false);
        getSharedPreferences().edit().putBoolean(
                LocationPolicyDisclaimer.KEY_HAS_AGREED_LOCATION_DISCLAIMER + TEST_SUB_ID, true)
                .commit();

        // Check the WFC disclaimer item is should not be shown due to an item is already agreed.
        boolean result = disclaimerItem.shouldShow();

        assertFalse(result);
    }

    @Test
    public void onAgreed_shouldSetSharedPreferencesToAgreed() {
        LocationPolicyDisclaimer disclaimerItem = spy(new LocationPolicyDisclaimer(mContext, 0));
        doReturn(TEST_SUB_ID).when(disclaimerItem).getSubscriptionId();

        disclaimerItem.onAgreed();

        // Check the SharedPreferences key is changed to agreed.
        boolean result = getSharedPreferences().getBoolean(
                LocationPolicyDisclaimer.KEY_HAS_AGREED_LOCATION_DISCLAIMER + TEST_SUB_ID,
                false);
        assertTrue(result);
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences("test_wfc_disclaimer_prefs", Context.MODE_PRIVATE);
    }
}

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

package com.android.settings.wifi.calling;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class EmergencyCallLimitationDisclaimerTest {
    private static final String TEST_SHARED_PREFERENCE = "test_wfc_disclaimer_prefs";
    private static final int TEST_SUB_ID = 0;

    @Mock
    TextView mMockView;
    @Mock
    TextView mMockTextView;

    private final PersistableBundle mBundle = new PersistableBundle();
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        LayoutInflater mockInflater = mock(LayoutInflater.class);
        doReturn(mockInflater).when(mContext).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        when(mockInflater.inflate(anyInt(), anyObject(), anyBoolean())).thenReturn(mMockView);
        when(mMockView.findViewById(R.id.disclaimer_desc)).thenReturn(mMockTextView);

        CarrierConfigManager mockConfigManager = mock(CarrierConfigManager.class);
        doReturn(mockConfigManager).when(mContext).getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        when(mockConfigManager.getConfigForSubId(anyInt())).thenReturn(mBundle);

        doReturn(getSharedPreferences()).when(mContext).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void constructor_shouldCreateEmergencyCallLimitationDisclaimerView() {
        EmergencyCallLimitationDisclaimer disclaimerItem = new EmergencyCallLimitationDisclaimer(
                mContext, TEST_SUB_ID);

        // Check the WFC disclaimer item view is created.
        verify(mMockTextView).setText(R.string.wfc_disclaimer_emergency_limitation_desc_txt);
        assertEquals(disclaimerItem.getView(), mMockView);
    }

    @Test
    public void sholdShow_delay1000msec_shouldShowEmergencyCallLimitationDisclaimer() {
        EmergencyCallLimitationDisclaimer disclaimerItem =
                spy(new EmergencyCallLimitationDisclaimer(mContext, TEST_SUB_ID));
        mBundle.putInt(CarrierConfigManager.KEY_EMERGENCY_NOTIFICATION_DELAY_INT, 1000);
        getSharedPreferences().edit().putBoolean(
                EmergencyCallLimitationDisclaimer.KEY_HAS_AGREED_EMERGENCY_LIMITATION_DISCLAIMER
                + TEST_SUB_ID, false).commit();

        boolean result = disclaimerItem.shouldShow();

        // Check the WFC disclaimer item is should be shown.
        assertTrue(result);
    }

    @Test
    public void sholdShow_delayDefault_shouldNotShowEmergencyCallLimitationDisclaimer() {
        EmergencyCallLimitationDisclaimer disclaimerItem = new EmergencyCallLimitationDisclaimer(
                mContext, TEST_SUB_ID);
        mBundle.putInt(CarrierConfigManager.KEY_EMERGENCY_NOTIFICATION_DELAY_INT, -1);

        boolean result = disclaimerItem.shouldShow();

        // Check the WFC disclaimer item is should not be shown due to the
        // KEY_EMERGENCY_NOTIFICATION_DELAY_INT on carrier config is default(-1).
        assertFalse(result);
    }

    @Test
    public void sholdShow_alreadyAgreed_shouldNotShowEmergencyCallLimitationDisclaimer() {
        EmergencyCallLimitationDisclaimer disclaimerItem =
                spy(new EmergencyCallLimitationDisclaimer(mContext, TEST_SUB_ID));
        mBundle.putInt(CarrierConfigManager.KEY_EMERGENCY_NOTIFICATION_DELAY_INT, 10);
        getSharedPreferences().edit().putBoolean(
                EmergencyCallLimitationDisclaimer.KEY_HAS_AGREED_EMERGENCY_LIMITATION_DISCLAIMER
                + TEST_SUB_ID, true).commit();

        // Check the WFC disclaimer item is should not be shown due to an item is already agreed.
        boolean result = disclaimerItem.shouldShow();

        assertFalse(result);
    }

    @Test
    public void onAgreed_shouldSetSharedPreferencesToAgreed() {
        EmergencyCallLimitationDisclaimer disclaimerItem =
                spy(new EmergencyCallLimitationDisclaimer(mContext, TEST_SUB_ID));

        disclaimerItem.onAgreed();

        // Check the SharedPreferences key is changed to agreed.
        boolean result = getSharedPreferences().getBoolean(
                EmergencyCallLimitationDisclaimer.KEY_HAS_AGREED_EMERGENCY_LIMITATION_DISCLAIMER
                + TEST_SUB_ID, false);
        assertTrue(result);
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(TEST_SHARED_PREFERENCE, Context.MODE_PRIVATE);
    }
}

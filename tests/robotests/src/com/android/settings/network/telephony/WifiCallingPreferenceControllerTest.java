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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsMmTelManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.ims.ImsQuery;
import com.android.settings.network.ims.ImsQueryResultTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowTelephonyManager;

@RunWith(AndroidJUnit4.class)
public class WifiCallingPreferenceControllerTest {
    private static final int SUB_ID = 2;

    private Context mContext;
    private ShadowContextImpl mShadowContextImpl;

    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mCarrierConfig;

    private ShadowTelephonyManager mShadowTelephonyManager;

    private ImsQuery mImsQueryTrue;
    private ImsQuery mImsQueryFalse;

    @Mock
    private PreferenceScreen mPreferenceScreen;
    private Preference mPreference;

    @Mock
    private PhoneAccountHandle mSimCallManager;
    @Mock
    private ImsMmTelManager mImsMmTelManager;
    private WifiCallingPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application.getBaseContext();
        mShadowContextImpl = Shadow.extract(mContext);

        mCarrierConfig = new PersistableBundle();
        mShadowContextImpl.setSystemService(Context.CARRIER_CONFIG_SERVICE, mCarrierConfigManager);
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(anyInt());

        mShadowTelephonyManager = shadowOf(mContext.getSystemService(TelephonyManager.class));
        mShadowTelephonyManager.setIsNetworkRoaming(false);

        mImsQueryTrue = new ImsQueryResultTest.ImsQueryBoolean(true);
        mImsQueryFalse = new ImsQueryResultTest.ImsQueryBoolean(false);

        mController = spy(new WifiCallingPreferenceController(mContext, "wifi_calling"));

        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        doReturn(mPreference).when(mPreferenceScreen).findPreference(any());

        doReturn(mImsMmTelManager).when(mController).getImsMmTelManager(anyInt());
        mController.mSimCallManager = mSimCallManager;
        mController.mCallState = TelephonyManager.CALL_STATE_IDLE;

        doReturn(mImsQueryTrue).when(mController).isWfcEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryTrue).when(mController).isWfcProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mController).isWfcEnabledByPlatform(anyInt());
    }

    @Test
    public void updateState_noSimCallManager_setCorrectSummary() {
        when(mImsMmTelManager.getVoWiFiRoamingModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);
        when(mImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);

        mController.init(SUB_ID);
        mController.mSimCallManager = null;

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(com.android.internal.R.string.wfc_mode_wifi_only_summary));
    }

    @Test
    public void updateState_notCallIdle_disable() {
        when(mImsMmTelManager.getVoWiFiRoamingModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);
        when(mImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);

        mController.init(SUB_ID);
        mController.mCallState = TelephonyManager.CALL_STATE_RINGING;

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_wfcNonRoamingByConfig() {
        mShadowTelephonyManager.setIsNetworkRoaming(true);

        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, true);

        when(mImsMmTelManager.getVoWiFiRoamingModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED);
        when(mImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED);

        mController.init(SUB_ID);

        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.wfc_mode_cellular_preferred_summary));
    }

    @Test
    public void updateState_wfcRoamingByConfig() {
        mShadowTelephonyManager.setIsNetworkRoaming(true);

        when(mImsMmTelManager.getVoWiFiRoamingModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED);
        when(mImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED);

        mController.init(SUB_ID);

        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.wfc_mode_wifi_preferred_summary));
    }

    @Test
    public void displayPreference_notAvailable_setPreferenceInvisible() {
        when(mImsMmTelManager.getVoWiFiRoamingModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);
        when(mImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);

        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_available_setsSubscriptionIdOnIntent() {
        when(mImsMmTelManager.getVoWiFiRoamingModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);
        when(mImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);

        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        final Intent intent = new Intent();
        mPreference.setIntent(intent);
        mController.displayPreference(mPreferenceScreen);
        assertThat(intent.getIntExtra(Settings.EXTRA_SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID)).isEqualTo(SUB_ID);
    }

    @Test
    public void getAvailabilityStatus_noWiFiCalling_shouldReturnUnsupported() {
        when(mImsMmTelManager.getVoWiFiRoamingModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);
        when(mImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);

        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }
}

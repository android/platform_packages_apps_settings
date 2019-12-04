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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;

import androidx.preference.SwitchPreference;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.ims.ImsQuery;
import com.android.settings.network.ims.ImsQueryResultTest;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContextImpl;

@RunWith(AndroidJUnit4.class)
public class Enhanced4gBasePreferenceControllerTest {
    private static final int SUB_ID = 2;

    private Context mContext;
    private ShadowContextImpl mShadowContextImpl;

    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mCarrierConfig;

    private ImsQuery mImsQueryTrue;
    private ImsQuery mImsQueryFalse;

    private Enhanced4gLtePreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application.getBaseContext();
        mShadowContextImpl = Shadow.extract(mContext);

        mCarrierConfig = new PersistableBundle();
        mShadowContextImpl.setSystemService(Context.CARRIER_CONFIG_SERVICE, mCarrierConfigManager);
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(anyInt());

        mImsQueryTrue = new ImsQueryResultTest.ImsQueryBoolean(true);
        mImsQueryFalse = new ImsQueryResultTest.ImsQueryBoolean(false);

        mController = spy(new Enhanced4gLtePreferenceController(mContext, "volte"));

        doReturn(mImsQueryTrue).when(mController).isSystemTtyEnabled();
        mPreference = new RestrictedSwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        mController.mPreference = mPreference;
    }

    @Test
    public void getAvailabilityStatus_default_returnUnavailable() {
        doReturn(mImsQueryFalse).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryFalse).when(mController).isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryFalse).when(mController).isVolteProvisionedOnDevice(anyInt());
        doReturn(mImsQueryFalse).when(mController).isVolteEnabledByPlatform(anyInt());

        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_volteDisabled_returnUnavailable() {
        doReturn(mImsQueryFalse).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryFalse).when(mController).isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVolteProvisionedOnDevice(anyInt());
        doReturn(mImsQueryFalse).when(mController).isVolteEnabledByPlatform(anyInt());

        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateState_configEnabled_prefEnabled() {
        mController.init(SUB_ID);

        mPreference.setEnabled(false);

        mCarrierConfig.putInt(CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT, 1);
        mController.mCallState = TelephonyManager.CALL_STATE_IDLE;
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true);

        doReturn(mImsQueryTrue).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryTrue).when(mController).isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVolteProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVolteEnabledByPlatform(anyInt());

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_configOn_prefChecked() {
        mController.init(SUB_ID);

        mPreference.setChecked(false);

        mCarrierConfig.putInt(CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT, 1);
        mController.mCallState = TelephonyManager.CALL_STATE_IDLE;
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true);

        doReturn(mImsQueryTrue).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryTrue).when(mController).isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVolteProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVolteEnabledByPlatform(anyInt());
        doReturn(mImsQueryTrue).when(mController).isEnhanced4gLteModeSettingEnabledByUser(
                anyInt());

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }
}

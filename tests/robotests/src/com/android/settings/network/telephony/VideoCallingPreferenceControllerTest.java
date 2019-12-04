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

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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

@RunWith(AndroidJUnit4.class)
public class VideoCallingPreferenceControllerTest {
    private static final int SUB_ID = 2;

    private Context mContext;
    private ShadowContextImpl mShadowContextImpl;

    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mCarrierConfig;

    private ImsQuery mImsQueryTrue;
    private ImsQuery mImsQueryFalse;

    @Mock
    private PreferenceScreen mPreferenceScreen;
    private SwitchPreference mPreference;

    private VideoCallingPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application.getBaseContext();
        mShadowContextImpl = Shadow.extract(mContext);

        mCarrierConfig = new PersistableBundle();
        mShadowContextImpl.setSystemService(Context.CARRIER_CONFIG_SERVICE, mCarrierConfigManager);
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(anyInt());

        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS, true);

        mImsQueryTrue = new ImsQueryResultTest.ImsQueryBoolean(true);
        mImsQueryFalse = new ImsQueryResultTest.ImsQueryBoolean(false);

        mController = spy(new VideoCallingPreferenceController(mContext, "video_calling"));

        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        doReturn(mPreference).when(mPreferenceScreen).findPreference(any());

        doReturn(mImsQueryTrue).when(mController).isSystemTtyEnabled();
        mController.mCallState = TelephonyManager.CALL_STATE_IDLE;
    }

    @Test
    public void isVideoCallEnabled_allFlagsOn_returnTrue() {
        doReturn(mImsQueryTrue).when(mController).isMobileDataEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isEnhanced4gLteModeSettingEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtEnabledByPlatform(anyInt());

        mController.init(SUB_ID);

        assertThat(mController.isVideoCallEnabled(SUB_ID)).isTrue();
    }

    @Test
    public void isVideoCallEnabled_disabledByPlatform_returnFalse() {
        doReturn(mImsQueryTrue).when(mController).isMobileDataEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isEnhanced4gLteModeSettingEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryFalse).when(mController).isVtProvisionedOnDevice(anyInt());
        doReturn(mImsQueryFalse).when(mController).isVtEnabledByPlatform(anyInt());

        mController.init(SUB_ID);

        assertThat(mController.isVideoCallEnabled(SUB_ID)).isFalse();
    }

    @Test
    public void isVideoCallEnabled_dataDisabled_returnFalse() {
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS, false);

        doReturn(mImsQueryFalse).when(mController).isMobileDataEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isEnhanced4gLteModeSettingEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtEnabledByPlatform(anyInt());

        mController.init(SUB_ID);

        assertThat(mController.isVideoCallEnabled(SUB_ID)).isFalse();
    }

    @Test
    public void updateState_4gLteOff_disabled() {
        doReturn(mImsQueryTrue).when(mController).isMobileDataEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryFalse).when(mController)
                .isEnhanced4gLteModeSettingEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtEnabledByPlatform(anyInt());

        mController.init(SUB_ID);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_4gLteOnWithoutCall_checked() {
        doReturn(mImsQueryTrue).when(mController).isMobileDataEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isEnhanced4gLteModeSettingEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtEnabledByPlatform(anyInt());

        mController.init(SUB_ID);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void displayPreference_notAvailable_setPreferenceInvisible() {
        doReturn(mImsQueryTrue).when(mController).isMobileDataEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mController).isEnhanced4gLteModeSettingEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtEnabledByUser(anyInt());
        doReturn(mImsQueryTrue).when(mController).isImsServiceStateReady(anyInt());
        doReturn(mImsQueryTrue).when(mController).isVtProvisionedOnDevice(anyInt());
        doReturn(mImsQueryFalse).when(mController).isVtEnabledByPlatform(anyInt());

        mController.init(SUB_ID);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.isVisible()).isFalse();
    }
}

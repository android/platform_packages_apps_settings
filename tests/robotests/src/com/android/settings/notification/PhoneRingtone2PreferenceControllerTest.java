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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.media.RingtoneManager;
import android.os.UserHandle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.DefaultRingtonePreference;
import com.android.settings.R;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import static org.mockito.ArgumentMatchers.anyInt;

@RunWith(RobolectricTestRunner.class)
public class PhoneRingtone2PreferenceControllerTest {

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelecomManager mTelecomManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DefaultRingtonePreference mPreference;

    private Context mContext;
    private PhoneRingtone2PreferenceController mController;

    private static final PhoneAccountHandle PHONE_ACCOUNT_HANDLE_1 = new PhoneAccountHandle(
            new ComponentName("pkg", "cls"), "id_1", UserHandle.of(0));
    private static final PhoneAccount PHONE_ACCOUNT_1 =
            new PhoneAccount.Builder(PHONE_ACCOUNT_HANDLE_1, "tel_acct1")
                    .setSupportedUriSchemes(Arrays.asList(PhoneAccount.SCHEME_TEL))
                    .build();

    private static final PhoneAccountHandle PHONE_ACCOUNT_HANDLE_2 = new PhoneAccountHandle(
            new ComponentName("pkg", "cls"), "id_2", UserHandle.of(0));
    private static final PhoneAccount PHONE_ACCOUNT_2 =
            new PhoneAccount.Builder(PHONE_ACCOUNT_HANDLE_2, "tel_acct2")
                    .setSupportedUriSchemes(Arrays.asList(PhoneAccount.SCHEME_TEL))
                    .build();

    private static final String PHONE_ACCOUNT_HANDLE_DISPLAY_NAME = "TEST";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.TELEPHONY_SERVICE, mTelephonyManager);
        shadowContext.setSystemService(Context.TELECOM_SERVICE, mTelecomManager);
        shadowContext.setSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE,
                mSubscriptionManager);
        mContext = RuntimeEnvironment.application;
        mController = new PhoneRingtone2PreferenceController(mContext);
    }

    @Test
    public void displayPreference_shouldSetPhoneAccountHandle() {
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        when(mTelecomManager.getCallCapablePhoneAccounts(true))
                .thenReturn(Arrays.asList(PHONE_ACCOUNT_HANDLE_1, PHONE_ACCOUNT_HANDLE_2));
        when(mTelecomManager.getPhoneAccount(PHONE_ACCOUNT_HANDLE_1))
                .thenReturn(PHONE_ACCOUNT_1);
        when(mTelecomManager.getPhoneAccount(PHONE_ACCOUNT_HANDLE_2))
                .thenReturn(PHONE_ACCOUNT_2);
        mController.displayPreference(mPreferenceScreen);

        verify(mPreference).setPhoneAccountHandle(PHONE_ACCOUNT_HANDLE_2);
    }

    @Test
    public void displayPreference_shouldUpdateTitle_when_MultiPhoneAccountHandle() {
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        when(mTelecomManager.getCallCapablePhoneAccounts(true))
                .thenReturn(Arrays.asList(PHONE_ACCOUNT_HANDLE_1, PHONE_ACCOUNT_HANDLE_2));
        when(mTelecomManager.getPhoneAccount(PHONE_ACCOUNT_HANDLE_1))
                .thenReturn(PHONE_ACCOUNT_1);
        when(mTelecomManager.getPhoneAccount(PHONE_ACCOUNT_HANDLE_2))
                .thenReturn(PHONE_ACCOUNT_2);
        when(mSubscriptionManager.getActiveSubscriptionInfo(anyInt()))
                .thenReturn(mSubscriptionInfo);
        when(mSubscriptionInfo.getDisplayName()).thenReturn(PHONE_ACCOUNT_HANDLE_DISPLAY_NAME);
        mController.displayPreference(mPreferenceScreen);

        verify(mPreference).setTitle(mContext.getString(R.string.ringtone_title)
                + " - " + PHONE_ACCOUNT_HANDLE_DISPLAY_NAME);
    }

    @Test
    public void isAvailable_notVoiceCapable_shouldReturnFalse() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_notMultiPhoneAccountHandle_shouldReturnFalse() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mTelecomManager.getCallCapablePhoneAccounts(true))
                .thenReturn(Arrays.asList(PHONE_ACCOUNT_HANDLE_1));
        when(mTelecomManager.getPhoneAccount(PHONE_ACCOUNT_HANDLE_1))
                .thenReturn(PHONE_ACCOUNT_1);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_VoiceCapable_and_MultiPhoneAccountHandle_shouldReturnTrue() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mTelecomManager.getCallCapablePhoneAccounts(true))
                .thenReturn(Arrays.asList(PHONE_ACCOUNT_HANDLE_1, PHONE_ACCOUNT_HANDLE_2));
        when(mTelecomManager.getPhoneAccount(PHONE_ACCOUNT_HANDLE_1))
                .thenReturn(PHONE_ACCOUNT_1);
        when(mTelecomManager.getPhoneAccount(PHONE_ACCOUNT_HANDLE_2))
                .thenReturn(PHONE_ACCOUNT_2);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getRingtoneType_shouldReturnRingtone() {
        assertThat(mController.getRingtoneType()).isEqualTo(RingtoneManager.TYPE_RINGTONE);
    }
}

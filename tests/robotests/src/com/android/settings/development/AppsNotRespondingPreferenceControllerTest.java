/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static com.android.settings.development.AppsNotRespondingPreferenceController
        .SETTING_VALUE_OFF;
import static com.android.settings.development.AppsNotRespondingPreferenceController
        .SETTING_VALUE_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class AppsNotRespondingPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private AppsNotRespondingPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new AppsNotRespondingPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabled_appsNotRespondingDialogShouldBeOn() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ANR_SHOW_BACKGROUND, -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChange_settingDisabled_appsNotRespondingDialogShouldBeOff() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ANR_SHOW_BACKGROUND, -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ANR_SHOW_BACKGROUND, SETTING_VALUE_OFF);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ANR_SHOW_BACKGROUND, SETTING_VALUE_ON);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ANR_SHOW_BACKGROUND, -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }
}

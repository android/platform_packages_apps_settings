/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class EnhancedConnectivityPreferenceControllerTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private EnhancedConnectivityPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new EnhancedConnectivityPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_enhanceConnectivity_shouldBeOn() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        assertThat(isSettingEnabled()).isTrue();
    }

    @Test
    public void onPreferenceChanged_enhanceConnectivity_shouldBeOff() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        assertThat(isSettingEnabled()).isFalse();
    }

    @Test
    public void updateState_preferenceShouldBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ENHANCED_CONNECTIVITY_ENABLED, 1 /* enabled */);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_preferenceShouldNotBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ENHANCED_CONNECTIVITY_ENABLED, 0 /* disabled */);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldEnablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(true);

        assertThat(isSettingEnabled()).isTrue();
    }

    @Test
    public void isAvailable_enhancedConnectivityShown_shouldReturnTrue() {
        enableEnhancedConnectivityPreference(true);

        boolean availability = mController.isAvailable();

        assertThat(availability).isTrue();
    }

    @Test
    public void isAvailable_enhancedConnectivityNotShown_shouldReturnFalse() {
        enableEnhancedConnectivityPreference(false);

        boolean availability = mController.isAvailable();

        assertThat(availability).isFalse();
    }

    private void enableEnhancedConnectivityPreference(boolean enable) {
        when(mContext.getResources().getBoolean(R.bool.config_show_enhanced_connectivity))
                .thenReturn(enable);
    }

    private boolean isSettingEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ENHANCED_CONNECTIVITY_ENABLED,
                EnhancedConnectivityPreferenceController.ENHANCED_CONNECTIVITY_ON
                /* default on */)
                == EnhancedConnectivityPreferenceController.ENHANCED_CONNECTIVITY_ON;
    }
}

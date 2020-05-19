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

package com.android.settings.gestures;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CameraLongPressPreferenceControllerTest {

    private static final String KEY_CAMERA_LONG_PRESS = "gesture_camera_long_press";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private CameraLongPressPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new CameraLongPressPreferenceController(mContext, KEY_CAMERA_LONG_PRESS);
    }

    @Test
    public void isSuggestionCompleted_trueWhenVisited() {
        when(mContext.getResources().getBoolean(anyInt())).thenReturn(true);
        when(mContext.getResources().getString(anyInt())).thenReturn("foo");
        final Context context = RuntimeEnvironment.application;
        final SharedPreferences prefs =
                new SuggestionFeatureProviderImpl(context).getSharedPrefs(context);
        prefs.edit().putBoolean(CameraLongPressSettings.PREF_KEY_SUGGESTION_COMPLETE, true).commit();

        assertThat(CameraLongPressPreferenceController.isSuggestionComplete(mContext, prefs))
                .isTrue();
    }

    @Test
    public void getAvailabilityStatus_notEnabled_UNSUPPORTED_ON_DEVICE() {
        // Mock com.android.internal.R.integer.config_cameraButtonLaunchEnabled = false
        // -> Not enabled on device
        when(mContext.getResources().getInteger(anyBoolean())).thenReturn(false);
        final int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_enabledOnDevice_AVAILABLE() {
        // Mock com.android.internal.R.integer.config_cameraButtonLaunchEnabled = true
        // -> Enabled on device
        when(mContext.getResources().getInteger(anyBoolean())).thenReturn(true);
        int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(AVAILABLE);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final CameraLongPressPreferenceController controller =
                new CameraLongPressPreferenceController(mContext, KEY_CAMERA_LONG_PRESS);
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final CameraLongPressPreferenceController controller =
                new CameraLongPressPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }
}

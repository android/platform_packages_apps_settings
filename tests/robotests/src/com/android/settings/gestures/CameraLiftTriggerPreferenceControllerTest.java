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

import static android.provider.Settings.Secure.CAMERA_LIFT_TRIGGER_ENABLED;

import static com.android.settings.gestures.CameraLiftTriggerPreferenceController.OFF;
import static com.android.settings.gestures.CameraLiftTriggerPreferenceController.ON;
import static com.android.settings.gestures.CameraLiftTriggerPreferenceController.isSuggestionComplete;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class CameraLiftTriggerPreferenceControllerTest {

    private Context mContext;
    private ContentResolver mContentResolver;
    private CameraLiftTriggerPreferenceController mController;
    private static final String KEY_LIFT_TRIGGER = "gesture_camera_lift_trigger";


    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mController = new CameraLiftTriggerPreferenceController(mContext, KEY_LIFT_TRIGGER);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void isChecked_configIsNotSet_shouldReturnTrue() {
        // Lift trigger is on by default
        // (but might be hidden if the sensor is not supported)
        mController = new CameraLiftTriggerPreferenceController(mContext, KEY_LIFT_TRIGGER);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_configIsSetOff_shouldReturnFalse() {
        // Set the setting to be disabled.
        Settings.Secure.putInt(mContentResolver, CAMERA_LIFT_TRIGGER_ENABLED, OFF);
        mController = new CameraLiftTriggerPreferenceController(mContext, KEY_LIFT_TRIGGER);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_configIsSetOn_shouldReturnTrue() {
        // Set the setting to be enabled.
        Settings.Secure.putInt(mContentResolver, CAMERA_LIFT_TRIGGER_ENABLED, ON);
        mController = new CameraLiftTriggerPreferenceController(mContext, KEY_LIFT_TRIGGER);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isSuggestionCompleted_cameraLiftTrigger_trueWhenNotAvailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_cameraLiftTriggerSensorType, -1);

        assertThat(isSuggestionComplete(mContext, null/* prefs */)).isTrue();
    }

    @Test
    public void isSuggestionCompleted_cameraLiftTrigger_falseWhenNotVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_cameraLiftTriggerSensorType, 10000);
        // No stored value in shared preferences if not visited yet.
        final SharedPreferences prefs =
                new SuggestionFeatureProviderImpl(mContext).getSharedPrefs(mContext);

        assertThat(isSuggestionComplete(mContext, prefs)).isFalse();
    }

    @Test
    public void isSuggestionCompleted_cameraLiftTrigger_trueWhenVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_cameraLiftTriggerSensorType, 10000);
        // No stored value in shared preferences if not visited yet.
        final SharedPreferences prefs =
                new SuggestionFeatureProviderImpl(mContext).getSharedPrefs(mContext);

        // Visit the setting
        prefs.edit().putBoolean(
                CameraLiftTriggerSettings.PREF_KEY_SUGGESTION_COMPLETE, true).commit();

        assertThat(isSuggestionComplete(mContext, prefs)).isTrue();
    }

    @Test
    public void getAvailabilityStatus_incorrectSensor_UNSUPPORTED_ON_DEVICE() {
        // SensorType == -1 -> Invalid sensor because the validator checks for != -1
        // see frameworks/base: GestureLauncherService.java
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_cameraLiftTriggerSensorType,
                -1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_sensorTypeEqualsZero_AVAILABLE() {
        // SensorType = 0 -> Valid sensor because any Int != -1 is valid
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_cameraLiftTriggerSensorType, 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_sensorTypeIsPositive_AVAILABLE() {
        // SensorType > 0 -> Valid sensor because any Int != -1 is valid
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_cameraLiftTriggerSensorType,
                10000);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void isSliceable_setCorrectKey_returnsTrue() {
        final CameraLiftTriggerPreferenceController controller =
                new CameraLiftTriggerPreferenceController(mContext, KEY_LIFT_TRIGGER);

        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceable_setIncorrectKey_returnsFalse() {
        final CameraLiftTriggerPreferenceController controller =
                new CameraLiftTriggerPreferenceController(mContext, "bad_key");

        assertThat(controller.isSliceable()).isFalse();
    }
}

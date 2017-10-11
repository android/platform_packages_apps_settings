/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 *
 */

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.support.v7.preference.PreferenceScreen;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDynamicIndexableContentMonitor;
import com.android.settingslib.BatteryInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
                ShadowDynamicIndexableContentMonitor.class
        })
public class BatteryHeaderPreferenceControllerTest {
    private static final int BATTERY_LEVEL = 60;
    private static final String TIME_LEFT = "2h30min";
    private static final String BATTERY_STATUS = "Charging";

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private BatteryInfo mBatteryInfo;
    private BatteryHeaderPreferenceController mController;
    private Context mContext;
    private BatteryMeterView mBatteryMeterView;
    private TextView mTimeText;
    private TextView mSummary;
    private LayoutPreference mBatteryLayoutPref;
    private Intent mBatteryIntent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mBatteryMeterView = new BatteryMeterView(mContext);
        mTimeText = new TextView(mContext);
        mSummary = new TextView(mContext);

        mBatteryIntent = new Intent();
        mBatteryIntent.putExtra(BatteryManager.EXTRA_LEVEL, BATTERY_LEVEL);
        mBatteryIntent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        doReturn(mBatteryIntent).when(mContext).registerReceiver(any(), any());

        mBatteryLayoutPref = new LayoutPreference(mContext, R.layout.battery_header);
        doReturn(mBatteryLayoutPref).when(mPreferenceScreen).findPreference(
                BatteryHeaderPreferenceController.KEY_BATTERY_HEADER);

        mBatteryInfo.batteryLevel = BATTERY_LEVEL;

        mController = new BatteryHeaderPreferenceController(mContext);
        mController.mBatteryMeterView = mBatteryMeterView;
        mController.mTimeText = mTimeText;
        mController.mSummary = mSummary;
    }

    @Test
    public void testDisplayPreference_displayBatteryLevel() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(((BatteryMeterView) mBatteryLayoutPref.findViewById(
                R.id.battery_header_icon)).getBatteryLevel()).isEqualTo(BATTERY_LEVEL);
        assertThat(((TextView) mBatteryLayoutPref.findViewById(
                R.id.battery_percent)).getText()).isEqualTo("60%");
    }

    @Test
    public void testUpdatePreference_hasRemainingTime_showRemainingLabel() {
        mBatteryInfo.remainingLabel = TIME_LEFT;

        mController.updateHeaderPreference(mBatteryInfo);

        assertThat(mSummary.getText()).isEqualTo(mBatteryInfo.remainingLabel);
    }

    @Test
    public void testUpdatePreference_updateBatteryInfo() {
        mBatteryInfo.remainingLabel = TIME_LEFT;
        mBatteryInfo.batteryLevel = BATTERY_LEVEL;
        mBatteryInfo.discharging = true;

        mController.updateHeaderPreference(mBatteryInfo);

        assertThat(mBatteryMeterView.mDrawable.getBatteryLevel()).isEqualTo(BATTERY_LEVEL);
        assertThat(mBatteryMeterView.mDrawable.getCharging()).isEqualTo(false);
    }

    @Test
    public void testUpdatePreference_noRemainingTime_showStatusLabel() {
        mBatteryInfo.remainingLabel = null;
        mBatteryInfo.statusLabel = BATTERY_STATUS;

        mController.updateHeaderPreference(mBatteryInfo);

        assertThat(mSummary.getText()).isEqualTo(BATTERY_STATUS);
    }
}

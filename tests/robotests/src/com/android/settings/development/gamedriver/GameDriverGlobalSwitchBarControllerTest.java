/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.gamedriver;

import static com.android.settings.development.gamedriver.GameDriverEnableForAllAppsPreferenceController.GAME_DRIVER_DEFAULT;
import static com.android.settings.development.gamedriver.GameDriverEnableForAllAppsPreferenceController.GAME_DRIVER_OFF;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.widget.SwitchWidgetController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GameDriverGlobalSwitchBarControllerTest {

    @Mock
    private SwitchBar mSwitchBar;
    @Mock
    private SwitchWidgetController mSwitchWidgetController;
    @Mock
    private GameDriverContentObserver mGameDriverContentObserver;

    private Context mContext;
    private ContentResolver mResolver;
    private GameDriverGlobalSwitchBarController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mResolver = mContext.getContentResolver();
    }

    @Test
    public void constructor_gameDriverOn_shouldCheckSwitchBar() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        mController = new GameDriverGlobalSwitchBarController(
                mContext, new SwitchBarController(mSwitchBar));

        verify(mSwitchBar).setChecked(true);
    }

    @Test
    public void constructor_gameDriverOff_shouldUncheckSwitchBar() {
        Settings.Global.putInt(mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_OFF);
        mController = new GameDriverGlobalSwitchBarController(
                mContext, new SwitchBarController(mSwitchBar));

        verify(mSwitchBar).setChecked(false);
    }

    @Test
    public void onStart_shouldStartListeningAndRegister() {
        mController = new GameDriverGlobalSwitchBarController(
                mContext, new SwitchBarController(mSwitchBar));
        mController.mSwitchWidgetController = mSwitchWidgetController;
        mController.mGameDriverContentObserver = mGameDriverContentObserver;
        mController.onStart();

        verify(mSwitchWidgetController).startListening();
        verify(mGameDriverContentObserver).register(mResolver);
    }

    @Test
    public void onStop_shouldStopListeningAndUnregister() {
        mController = new GameDriverGlobalSwitchBarController(
                mContext, new SwitchBarController(mSwitchBar));
        mController.mSwitchWidgetController = mSwitchWidgetController;
        mController.mGameDriverContentObserver = mGameDriverContentObserver;
        mController.onStop();

        verify(mSwitchWidgetController).stopListening();
        verify(mGameDriverContentObserver).unregister(mResolver);
    }

    @Test
    public void onSwitchToggled_checked_shouldTurnOnGameDriver() {
        Settings.Global.putInt(mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_OFF);
        mController = new GameDriverGlobalSwitchBarController(
                mContext, new SwitchBarController(mSwitchBar));
        mController.onSwitchToggled(true);

        assertThat(Settings.Global.getInt(
                           mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT))
                .isEqualTo(GAME_DRIVER_DEFAULT);
    }

    @Test
    public void onSwitchToggled_unchecked_shouldTurnOffGameDriver() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        mController = new GameDriverGlobalSwitchBarController(
                mContext, new SwitchBarController(mSwitchBar));
        mController.onSwitchToggled(false);

        assertThat(Settings.Global.getInt(
                           mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT))
                .isEqualTo(GAME_DRIVER_OFF);
    }
}

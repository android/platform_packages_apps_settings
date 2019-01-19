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

package com.android.settings.development.gup;

import static com.android.settings.development.gup.GupEnableForAllAppsPreferenceController.GUP_ALL_APPS;
import static com.android.settings.development.gup.GupEnableForAllAppsPreferenceController.GUP_DEFAULT;
import static com.android.settings.development.gup.GupEnableForAllAppsPreferenceController.GUP_OFF;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

/**
 * Controller of global switch bar used to fully turn off Game Driver.
 */
public class GupGlobalSwitchBarController
        implements SwitchWidgetController.OnSwitchChangeListener,
                   GameDriverContentObserver.OnGameDriverContentChangedListener, LifecycleObserver,
                   OnStart, OnStop {
    private final Context mContext;
    private final ContentResolver mContentResolver;
    @VisibleForTesting
    SwitchWidgetController mSwitchWidgetController;
    @VisibleForTesting
    GameDriverContentObserver mGameDriverContentObserver;

    GupGlobalSwitchBarController(Context context, SwitchWidgetController switchWidgetController) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mGameDriverContentObserver =
                new GameDriverContentObserver(new Handler(Looper.getMainLooper()), this);
        mSwitchWidgetController = switchWidgetController;
        mSwitchWidgetController.setEnabled(
                DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context));
        mSwitchWidgetController.setChecked(Settings.Global.getInt(mContentResolver,
                                                   Settings.Global.GUP_DEV_ALL_APPS, GUP_DEFAULT)
                != GUP_OFF);
        mSwitchWidgetController.setListener(this);
    }

    @Override
    public void onStart() {
        mSwitchWidgetController.startListening();
        mGameDriverContentObserver.register(mContentResolver);
    }

    @Override
    public void onStop() {
        mSwitchWidgetController.stopListening();
        mGameDriverContentObserver.unregister(mContentResolver);
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        if (!isChecked) {
            Settings.Global.putInt(mContentResolver, Settings.Global.GUP_DEV_ALL_APPS, GUP_OFF);
            return true;
        }

        if (Settings.Global.getInt(mContentResolver, Settings.Global.GUP_DEV_ALL_APPS, GUP_DEFAULT)
                != GUP_ALL_APPS) {
            Settings.Global.putInt(mContentResolver, Settings.Global.GUP_DEV_ALL_APPS, GUP_DEFAULT);
        }

        return true;
    }

    @Override
    public void onGameDriverContentChanged() {
        mSwitchWidgetController.setChecked(Settings.Global.getInt(mContentResolver,
                                                   Settings.Global.GUP_DEV_ALL_APPS, GUP_DEFAULT)
                != GUP_OFF);
    }
}

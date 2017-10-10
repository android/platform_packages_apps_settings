/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import android.content.Context;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.AutoBrightnessPreferenceController;
import com.android.settings.display.AutoRotatePreferenceController;
import com.android.settings.display.CameraGesturePreferenceController;
import com.android.settings.display.DozePreferenceController;
import com.android.settings.display.FontSizePreferenceController;
import com.android.settings.display.LiftToWakePreferenceController;
import com.android.settings.display.NightDisplayPreferenceController;
import com.android.settings.display.NightModePreferenceController;
import com.android.settings.display.ScreenSaverPreferenceController;
import com.android.settings.display.ShowOperatorNamePreferenceController;
import com.android.settings.display.TapToWakePreferenceController;
import com.android.settings.display.ThemePreferenceController;
import com.android.settings.display.TimeoutPreferenceController;
import com.android.settings.display.VrDisplayPreferenceController;
import com.android.settings.display.WallpaperPreferenceController;
import com.android.settings.gestures.DoubleTapScreenPreferenceController;
import com.android.settings.gestures.PickupGesturePreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

public class DisplaySettings extends DashboardFragment {
    private static final String TAG = "DisplaySettings";

    public static final String KEY_DISPLAY_SIZE = "screen_zoom";

    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_PICK_UP = "gesture_pick_up_display_summary";
    private static final String KEY_DOUBLE_TAP_SCREEN = "gesture_double_tap_screen_display_summary";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DISPLAY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mProgressiveDisclosureMixin.setTileLimit(4);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.display_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    private static List<PreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new AutoBrightnessPreferenceController(context, KEY_AUTO_BRIGHTNESS));
        controllers.add(new AutoRotatePreferenceController(context));
        controllers.add(new CameraGesturePreferenceController(context));
        controllers.add(new DozePreferenceController(context));
        controllers.add(new FontSizePreferenceController(context));
        controllers.add(new LiftToWakePreferenceController(context));
        controllers.add(new NightDisplayPreferenceController(context));
        controllers.add(new NightModePreferenceController(context));
        controllers.add(new ScreenSaverPreferenceController(context));
        controllers.add(new ShowOperatorNamePreferenceController(context));
        AmbientDisplayConfiguration ambientDisplayConfig = new AmbientDisplayConfiguration(context);
        controllers.add(new PickupGesturePreferenceController(
                context, lifecycle, ambientDisplayConfig, UserHandle.myUserId(), KEY_PICK_UP));
        controllers.add(new DoubleTapScreenPreferenceController(
                context, lifecycle, ambientDisplayConfig, UserHandle.myUserId(),
                KEY_DOUBLE_TAP_SCREEN));
        controllers.add(new TapToWakePreferenceController(context));
        controllers.add(new TimeoutPreferenceController(context, KEY_SCREEN_TIMEOUT));
        controllers.add(new VrDisplayPreferenceController(context));
        controllers.add(new WallpaperPreferenceController(context));
        controllers.add(new ThemePreferenceController(context));
        return controllers;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(KEY_DISPLAY_SIZE);
                    return keys;
                }

                @Override
                public List<PreferenceController> getPreferenceControllers(Context context) {
                    return buildPreferenceControllers(context, null);
                }
            };
}

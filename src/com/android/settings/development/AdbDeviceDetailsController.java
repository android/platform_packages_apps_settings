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
package com.android.settings.development;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.ActionButtonPreference;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.FooterPreference;

import com.android.settings.development.tests.WirelessDebuggingManager;
import com.android.settings.development.tests.WirelessDebuggingManager.PairedDevice;

/**
 * Controller for logic pertaining to displaying adb device information for the
 * {@link AdbDeviceDetailsFragment}.
 */
public class AdbDeviceDetailsController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnPause,
        OnResume {

    private static final String TAG = "AdbDeviceDetailsController";

    @VisibleForTesting
    static final String KEY_HEADER = "adb_device_header";
    @VisibleForTesting
    static final String KEY_BUTTONS_PREF = "buttons";
    @VisibleForTesting
    static final String KEY_MAC_ADDR_CATEGORY = "mac_addr_category";

    private PairedDevice mPairedDevice;
    private final Fragment mFragment;

    // UI elements - in order of appearance
    private ActionButtonPreference mButtonsPref;
    private EntityHeaderController mEntityHeaderController;
    private PreferenceCategory mMacAddrCategory;
    private FooterPreference mMacAddrPref;

    private final IconInjector mIconInjector;

    public static  AdbDeviceDetailsController newInstance(
            PairedDevice pairedDevice,
            Context context,
            Fragment fragment,
            Lifecycle lifecycle) {
        return new AdbDeviceDetailsController(
                pairedDevice, context, fragment, lifecycle,
                new IconInjector(context));
    }

    @VisibleForTesting
        /* package */ AdbDeviceDetailsController(
            PairedDevice pairedDevice,
            Context context,
            Fragment fragment,
            Lifecycle lifecycle,
            IconInjector injector) {
        super(context);

        mPairedDevice = pairedDevice;
        mFragment = fragment;
        mIconInjector = injector;

        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        // Returns null since this controller contains more than one Preference
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        setupEntityHeader(screen);

        mButtonsPref = ((ActionButtonPreference) screen.findPreference(KEY_BUTTONS_PREF))
                .setButton1Visible(false)
                .setButton2Text(R.string.adb_device_forget)
                .setButton2Positive(false)
                .setButton2OnClickListener(view -> forgetDevice());

        mMacAddrCategory = (PreferenceCategory) screen.findPreference(KEY_MAC_ADDR_CATEGORY);

        mMacAddrPref =
                new FooterPreference(mMacAddrCategory.getContext());
        final CharSequence titleFormat = mContext.getText(R.string.adb_device_mac_addr_title_format);
        mMacAddrPref.setTitle(String.format(
                titleFormat.toString(), mPairedDevice.getMacAddress()));
        mMacAddrCategory.addPreference(mMacAddrPref);
    }

    private void setupEntityHeader(PreferenceScreen screen) {
        LayoutPreference headerPref = (LayoutPreference) screen.findPreference(KEY_HEADER);
        mEntityHeaderController =
                EntityHeaderController.newInstance(
                        mFragment.getActivity(), mFragment,
                        headerPref.findViewById(R.id.entity_header));

        mEntityHeaderController.setIcon(
                mContext.getDrawable(com.android.internal.R.drawable.ic_bt_laptop));
        mEntityHeaderController.setLabel(mPairedDevice.getDeviceName());
        mEntityHeaderController.done(mFragment.getActivity(), true);
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    /**
     * Forgets the device.
     */
    private void forgetDevice() {
        Intent intent = new Intent();
        intent.putExtra(
                WirelessDebugging.PAIRED_DEVICE_REQUEST_TYPE,
                WirelessDebugging.FORGET_ACTION);
        intent.putExtra(
                WirelessDebugging.PAIRED_DEVICE_EXTRA,
                mPairedDevice);
        mFragment.getActivity().setResult(Activity.RESULT_OK, intent);
        mFragment.getActivity().finish();
    }

    /**
     * Wrapper for testing compatibility.
     */
    @VisibleForTesting
    static class IconInjector {
        private final Context mContext;

        public IconInjector(Context context) {
            mContext = context;
        }

        public Drawable getIcon(int level) {
            return mContext.getDrawable(Utils.getWifiIconResource(level)).mutate();
        }
    }
}


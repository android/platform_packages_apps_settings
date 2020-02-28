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

package com.android.settings.network.telephony;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsRcsManager;
import android.telephony.ims.RcsUceAdapter;
import android.util.Log;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

public class ContactDiscoveryPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver {
    private static final String TAG = "ContactDiscoveryPref";
    private static final String DIALOG_TAG = "ContactDiscoveryDialog";
    private static final Uri UCE_URI = Uri.withAppendedPath(Telephony.SimInfo.CONTENT_URI,
            Telephony.SimInfo.IMS_RCS_UCE_ENABLED);

    private ImsManager mImsManager;
    private CarrierConfigManager mCarrierConfigManager;
    private ContentObserver mUceSettingObserver;
    private Preference mPreference;
    private FragmentManager mFragmentManager;

    public ContactDiscoveryPreferenceController(Context context, String key) {
        super(context, key);
        mImsManager = mContext.getSystemService(ImsManager.class);
        mCarrierConfigManager = mContext.getSystemService(CarrierConfigManager.class);
    }

    public ContactDiscoveryPreferenceController init(FragmentManager fragmentManager, int subId,
            Lifecycle lifecycle) {
        mFragmentManager = fragmentManager;
        mSubId = subId;
        lifecycle.addObserver(this);
        return this;
    }

    @Override
    public boolean isChecked() {
        return isDiscoveryEnabled();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        registerUceObserver();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        unregisterUceObserver();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            showContentDiscoveryDialog();
            // launch dialog and wait for activity to return and ContentObserver to fire to update.
            return false;
        }
        setDiscoveryEnabled(false);
        return true;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        PersistableBundle bundle = mCarrierConfigManager.getConfigForSubId(subId);
        boolean shouldShowPresence = bundle.getBoolean(
                CarrierConfigManager.KEY_USE_RCS_PRESENCE_BOOL, false /*default*/);
        return shouldShowPresence ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    private void registerUceObserver() {
        mUceSettingObserver = new ContentObserver(mContext.getMainThreadHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null /*uri*/);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                Log.d(TAG, "UCE setting changed, re-evaluating.");
                SwitchPreference preference = (SwitchPreference) mPreference;
                preference.setChecked(isChecked());
            }
        };
        mContext.getContentResolver().registerContentObserver(UCE_URI, true /*notifyForDecendents*/,
                mUceSettingObserver);
    }

    private void unregisterUceObserver() {
        mContext.getContentResolver().unregisterContentObserver(mUceSettingObserver);
    }

    private void showContentDiscoveryDialog() {
        ContactDiscoveryDialogFragment dialog = ContactDiscoveryDialogFragment.newInstance(
                mSubId);
        dialog.show(mFragmentManager, DIALOG_TAG);
    }

    private void setDiscoveryEnabled(boolean isEnabled) {
        ImsRcsManager manager = getImsRcsManager();
        if (manager == null) return;
        RcsUceAdapter adapter = manager.getUceAdapter();
        try {
            adapter.setUceSettingEnabled(isEnabled);
        } catch (ImsException e) {
            Log.w(TAG, "UCE service is not available: " + e.getMessage());
        }
    }

    private boolean isDiscoveryEnabled() {
        ImsRcsManager manager = getImsRcsManager();
        if (manager == null) return false;
        RcsUceAdapter adapter = manager.getUceAdapter();
        try {
            return adapter.isUceSettingEnabled();
        } catch (ImsException e) {
            Log.w(TAG, "UCE service is not available: " + e.getMessage());
        }
        return false;
    }

    private ImsRcsManager getImsRcsManager() {
        if (mImsManager == null) return null;
        try {
            return mImsManager.getImsRcsManager(mSubId);
        } catch (Exception e) {
            Log.w(TAG, "Could not resolve ImsMmTelManager: " + e.getMessage());
        }
        return null;
    }
}

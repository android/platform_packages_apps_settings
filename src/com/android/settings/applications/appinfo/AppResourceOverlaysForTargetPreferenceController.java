/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.format.Formatter;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * AppResourceOverlaysForTargetPreferenceController controls the "Resource Overlays" line
 * items which summarize the runtime resource overlays installed for the given application
 * (target).
 */
public class AppResourceOverlaysForTargetPreferenceController extends BasePreferenceController {
    private static final String TAG = "RROForTargetPreference";
    private static final String KEY_RRO_FOR_TARGET = "resource_overlays";

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final int mUserId;

    private AppInfoDashboardFragment mParent;
    private Preference mPreference;

    public AppResourceOverlaysForTargetPreferenceController(Context context) {
        super(context, KEY_RRO_FOR_TARGET);
        mUserId = UserHandle.myUserId();
        mMetricsFeatureProvider = FeatureFactory.getFactory(context)
            .getMetricsFeatureProvider();
    }

    public void setParentFragment(AppInfoDashboardFragment parent) {
        mParent = parent;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreference = screen.findPreference(KEY_RRO_FOR_TARGET);
        final ApplicationsState.AppEntry appEntry = mParent.getAppEntry();
        if (appEntry == null || appEntry.info == null) {
            mPreference.setVisible(false);
        } else {
            // show on all types of packages except on an RRO package
            mPreference.setVisible(!appEntry.info.isResourceOverlay());
        }
    }

    @Override
    public void updateState(Preference preference) {
        List<String> overlays = getResourceOverlayPackageNames();
        setSummaryForOverlayPackages(overlays);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference == null) {
            return false;
        }

        if (!KEY_RRO_FOR_TARGET.equals(preference.getKey())) {
            return false;
        }

        Intent intent = getAppEntryIntent();
        if (intent == null) {
            return false;
        }

        return launchIntent(intent);
    }

    private List<String> getResourceOverlayPackageNames() {
        final IOverlayManager oms = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
        final int uid = UserHandle.getUserId(mUserId);
        List<OverlayInfo> overlayInfos = null;
        try {
            overlayInfos = oms
                .getOverlayInfosForTarget(mParent.getAppEntry().info.packageName,
                                          uid);
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }

        List<String> overlayPkgNames = new ArrayList<>();
        if (overlayInfos != null) {
            for (OverlayInfo overlay: overlayInfos) {
                overlayPkgNames.add(overlay.packageName);
            }
        }

        return overlayPkgNames;
    }

    private void setSummaryForOverlayPackages(List<String> overlays) {
        final Resources res = mContext.getResources();

        CharSequence summary;
        if (overlays == null || overlays.isEmpty()) {
            summary = res.getString(R.string.resource_overlays_applied_none);
            mPreference.setEnabled(false);
        } else {
            long totalSize = 0;
            for (String pkgName: overlays) {
                ApplicationsState appState =
                        ApplicationsState.getInstance(mParent
                                .getActivity().getApplication());
                ApplicationsState.AppEntry entry = appState.getEntry(pkgName,
                                                                     mUserId);
                totalSize += entry.internalSize;

            }
            summary = Formatter.formatFileSize(mContext, totalSize);
            mPreference.setEnabled(true);
        }
        mPreference.setSummary(summary);
    }

    private Intent getAppEntryIntent() {
        final Bundle args = new Bundle(2);
        args.putString(ManageApplications.EXTRA_RRO_TARGET,
                       mParent.getAppEntry().info.packageName);
        args.putInt(Intent.EXTRA_USER_ID, mUserId);
        return new SubSettingLauncher(mContext)
                .setDestination(ManageApplications.class.getName())
                .setTitleRes(R.string.resource_overlays_title)
                .setArguments(args)
                .setSourceMetricsCategory(mMetricsFeatureProvider
                                              .getMetricsCategory(mParent))
                .toIntent();
    }

    private boolean launchIntent(Intent intent) {
        try {
            mParent.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No activity found for " + intent);
        }

        return false;
    }
}

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

import android.content.Context;
import android.os.UserHandle;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.applications.ApplicationsState;

/**
 * AppResourceOverlayPreferenceController controls the "Resource Overlays target" line
 * item within AppInfo of Runtime Resource Overlay package. If its target package
 * is installed, the summary displays the name of the target, and if the target package
 * is not installed the summary also adds a note about missing target.
 */
public class AppResourceOverlayPreferenceController extends BasePreferenceController {
    private static final String TAG = "AppResourceOverlayPreferenceController";
    private static final String KEY_RESOURCE_OVERLAY = "resource_overlay_target";

    // constant value that can be used to check return code from sub activity.
    // (from com.android.settings.applications.manageapplications.ManageApplications)
    private static final int INSTALLED_APP_DETAILS = 1;

    private AppInfoDashboardFragment mParent;
    private ApplicationsState mState;
    private int mCurrentUid;
    private Preference mPreference;
    private String mTargetPkgName;

    public AppResourceOverlayPreferenceController(Context context) {
        super(context, KEY_RESOURCE_OVERLAY);
    }

    public void setParentFragment(AppInfoDashboardFragment parent) {
        mParent = parent;
        mTargetPkgName = mParent.getPackageInfo().overlayTarget;
    }

    public void setState(ApplicationsState state) {
        mState = state;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());

        final ApplicationsState.AppEntry appEntry = mParent.getAppEntry();
        if (appEntry == null || appEntry.info == null) {
            mPreference.setVisible(false);
        } else if (!appEntry.info.isResourceOverlay()) {
            mPreference.setVisible(false);
        } else {
            mPreference.setVisible(true);
        }
    }

    @Override
    public void updateState(Preference preference) {
        final ApplicationsState.AppEntry appEntry = mParent.getAppEntry();
        if (appEntry != null
                && appEntry.info != null
                && appEntry.info.isResourceOverlay()) {
            mCurrentUid = appEntry.info.uid;
            ApplicationsState.AppEntry targetEntry = mState.getEntry(
                                                         mTargetPkgName,
                                                         UserHandle.myUserId());
            if (targetEntry == null) {
                String summary = mTargetPkgName
                        + " ("
                        + mParent.getActivity()
                            .getString(R.string.resource_overlay_target_summary_missing)
                        + ")";
                preference.setSummary(summary);
            } else {
                preference.setSummary(targetEntry.label);
            }
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!KEY_RESOURCE_OVERLAY.equals(preference.getKey())) {
            return false;
        }

        AppInfoBase.startAppInfoFragment(AppInfoDashboardFragment.class,
                                         R.string.application_info_label,
                                         mTargetPkgName,
                                         mCurrentUid,
                                         mParent,
                                         INSTALLED_APP_DETAILS,
                                         MetricsEvent.MANAGE_APPLICATIONS);
        return true;
    }

    /**
     * UI needs to be refreshed with updated state.
     */
    public void refreshUi() {
        updateState(mPreference);
    }
}

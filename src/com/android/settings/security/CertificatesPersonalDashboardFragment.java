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

package com.android.settings.security;

import static android.provider.Settings.EXTRA_AUTHORITIES;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.users.AutoSyncDataPreferenceController;
import com.android.settings.users.AutoSyncPersonalDataPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage Certificates Setting page for personal profile.
 */
@SearchIndexable
public class CertificatesPersonalDashboardFragment extends DashboardFragment {

    private static final String TAG = "CertificatesPersonalFrag";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MANAGE_CERTIFICATES;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.certificates_personal_dashboard;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_certificates;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final String[] authorities = getIntent().getStringArrayExtra(EXTRA_AUTHORITIES);
        return buildPreferenceControllers(context, this /* parent */, authorities);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            SettingsPreferenceFragment parent, String[] authorities) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        final CertificatesPreferenceController certPrefController =
                new CertificatesPreferenceController(context, parent, authorities,
                        ProfileSelectFragment.PERSONAL);
        if (parent != null) {
            parent.getSettingsLifecycle().addObserver(certPrefController);
        }
        controllers.add(certPrefController);
        controllers.add(new AutoSyncDataPreferenceController(context, parent));
        controllers.add(new AutoSyncPersonalDataPreferenceController(context, parent));
        return controllers;
    }
}
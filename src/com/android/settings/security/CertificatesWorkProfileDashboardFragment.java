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

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment containing credentials controllers for work profile on managed device
 */
@SearchIndexable
public class CertificatesWorkProfileDashboardFragment extends DashboardFragment {

    private static final String TAG = "AccountWorkProfileFrag";

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
        return R.xml.certificates_work_profile_dashboard;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_certificates;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            SettingsPreferenceFragment parent, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        controllers.add(new CertificatesPreferenceController(context, parent,
                ProfileSelectFragment.WORK, lifecycle));
        controllers.add(new ResetCredentialsPreferenceController(context,
                ProfileSelectFragment.WORK, lifecycle));
        controllers.add(new InstallCertificatePreferenceController(context));
        return controllers;
    }
}

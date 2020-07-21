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

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.Arrays;
import java.util.List;

/**
 * Fragment containing credentials controllers for unmanaged device.
 * Equivalent for managed device linked via ProfileFragmentBridge
 * (CertificatesProfileSelectFragment).
 */
@SearchIndexable
public class CertificatesAllDashboardFragment extends CertificatesDashboardFragment {

    @Override
    protected int getType() {
        return ProfileSelectFragment.ALL;
    }

    @Override
    protected String getLogTag() {
        return "CertificatesAllDashboardFrag";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.certificates_personal_dashboard;
    }

    public final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.certificates_personal_dashboard;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(
                            context, null , null);
                }
            };
}

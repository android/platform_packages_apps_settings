/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 */

package com.android.settings.search;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

public class FakeIndexProvider implements Indexable {

    public static final String KEY = "TestKey";

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    return null;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> result = super.getNonIndexableKeys(context);
                    result.add(KEY);
                    return result;
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
                    return null;
                }
            };

}

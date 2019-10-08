/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.utils;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LocalClassLoaderContextThemeWrapperTest {

    private LocalClassLoaderContextThemeWrapper mContextThemeWrapper;

    @Test
    public void getClassLoader_shouldUseLocalClassLoader() {
        final Context context = RuntimeEnvironment.application;
        final Class clazz = LocalClassLoaderContextThemeWrapperTest.class;
        mContextThemeWrapper = new LocalClassLoaderContextThemeWrapper(clazz, context, 0);

        assertThat(mContextThemeWrapper.getClassLoader()).isSameAs(clazz.getClassLoader());
    }
}

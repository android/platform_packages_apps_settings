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

package com.android.settings.deviceinfo.hardwareinfo;


import static com.google.common.truth.Truth.assertThat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SerialNumberPreferenceControllerTest {

    private Context mContext;
    private SerialNumberPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new SerialNumberPreferenceController(mContext, "test");
    }

    @Test
    public void isCopyableSlice() {
        assertThat(mController.isSliceable()).isTrue();
        assertThat(mController.isCopyableSlice()).isTrue();
    }

    @Test
    public void copy_shouldPutSerialNumberToClipBoard() {
        mController.copy();

        final ClipboardManager clipboardManager = mContext.getSystemService(ClipboardManager.class);
        final ClipData data = clipboardManager.getPrimaryClip();

        assertThat(data.getItemAt(0).getText().toString()).contains(Build.getSerial());
    }
}

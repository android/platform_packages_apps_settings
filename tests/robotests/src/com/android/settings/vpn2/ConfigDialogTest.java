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

package com.android.settings.vpn2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.security.KeyStore;
import android.widget.Spinner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ConfigDialogTest {
    @Spy private ConfigDialog mSpyConfigDialog;
    @Mock private KeyStore mMockKeyStore;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        mMockKeyStore = mock(KeyStore.class);
        mSpyConfigDialog = spy(new ConfigDialog(mContext, null /* listener */,
                null /* profile */, true /* editing */, false /* exists */));
        when(mSpyConfigDialog.getKeyStore()).thenReturn(mMockKeyStore);
    }

    @Test
    public void loadCertificates_undesiredCertificates_shouldNotLoadUndesiredCertificates() {
        final Spinner spinner = new Spinner(mContext);
        when(mMockKeyStore.list(anyString())).thenReturn(ConfigDialog.UNDESIRED_CERTIFICATES);

        mSpyConfigDialog.loadCertificates(spinner,
                "prefix",
                0 /* firstId */,
                null /* selected */);

        assertThat(spinner.getAdapter().getCount()).isEqualTo(1);   // The String of firstId.
    }
}

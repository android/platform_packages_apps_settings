/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.NetworkPolicyManager;
import android.util.SparseIntArray;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DataSaverBackendTest {
    private NetworkPolicyManager mNetworkPolicyManager;
    private Context mContext;
    private DataSaverBackend mBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        mNetworkPolicyManager = mock(NetworkPolicyManager.class);
        shadowContext.setSystemService(Context.NETWORK_POLICY_SERVICE, mNetworkPolicyManager);
        mContext = shadowContext.getApplicationContext();
        mBackend = new DataSaverBackend(mContext);
    }

    @Test
    public void refreshWhitelist_shouldUpdateWhitelistedCount() {
        SparseIntArray uidPolicies = new SparseIntArray();
        final int policy = NetworkPolicyManager.POLICY_ALLOW_METERED_BACKGROUND;
        ReflectionHelpers.setField(mBackend, "mPolicyManager", mNetworkPolicyManager);
        ReflectionHelpers.setField(mBackend, "mUidPolicies", uidPolicies);

        int testUidNull[] = {};
        when(mNetworkPolicyManager.getUidsWithPolicy(policy)).thenReturn(testUidNull);
        mBackend.refreshWhitelist();
        assertThat(uidPolicies.size()).isEqualTo(0);

        int testAddUid[] = {123123,123126};
        when(mNetworkPolicyManager.getUidsWithPolicy(policy)).thenReturn(testAddUid);
        mBackend.refreshWhitelist();
        assertThat(uidPolicies.size()).isEqualTo(testAddUid.length);

        int testRemoveUid[] = {123123};
        when(mNetworkPolicyManager.getUidsWithPolicy(policy)).thenReturn(testRemoveUid);
        mBackend.refreshWhitelist();
        assertThat(uidPolicies.size()).isEqualTo(testRemoveUid.length);
    }

}


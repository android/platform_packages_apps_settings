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

package com.android.settings.security.credentials;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.IKeyChainService;
import android.security.KeyChain;

import androidx.fragment.app.Fragment;

import com.android.settings.TrustedCredentialsDetailsPreference;
import com.android.settings.core.SubSettingLauncher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowLog;

import java.security.cert.X509Certificate;


@RunWith(RobolectricTestRunner.class)
public class TrustedCredentialsDetailsPreferenceTest {

    private static final String TAG = "TrustedCredentialsDetailsPreferenceTest";

    private Context mContext;
    private Activity mActivity;
    @Mock private Fragment mFragment;
    @Mock private IKeyChainService.Stub mService;
    private String mAlias;
    private int mProfileId;
    private X509Certificate mCert;
    private SslCertificate mSsl;

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        setBinding(mService);
    }

    @Test
    public void testKeyChainServicePassedData() {
        try {
            when(mService.getEncodedCaCertificate(mAlias, true)).thenReturn(CA_CERTIFICATE);
            when(mService.containsCaAlias(mAlias)).thenReturn(true);

            mCert = KeyChain.toCertificate(CA_CERTIFICATE);
            mActivity = setUpData();

//            LinearLayout layout = activity.findViewById(R.id.sslcert_details);
            assertThat(mActivity.getTitle()).isEqualTo("CName title");

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setBinding(IKeyChainService.Stub binder) {
        Intent intent = new Intent("android.security.IKeyChainService");
        IKeyChainService.Stub actualServiceStub = (IKeyChainService.Stub) binder;

        ComponentName serviceClassComponentName = new ComponentName("com.android.keychain",
                "com.android.keychain.KeyChainService");

        shadowOf((Application) mContext.getApplicationContext())
                .setComponentNameAndServiceForBindServiceForIntent(
                        intent, serviceClassComponentName, actualServiceStub);
    }

    public Activity setUpData() {
        Bundle args = new Bundle();
        args.putString("alias", "testAlias");
        args.putInt("profileId", 100);

        Intent intent = spy(new SubSettingLauncher(mContext))
                .setDestination(TrustedCredentialsDetailsPreference.class.getName())
                .setSourceMetricsCategory(123)
                .setArguments(args)
                .setUserHandle(new UserHandle(args.getInt("profileId")))
                .setResultListener(mFragment, 1)
                .toIntent();

        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class, intent);

        Activity activity = controller
                .create()
                .start()
                .resume()
                .visible()
                .get();

        mAlias = intent.getExtras().getString("alias");
        mProfileId = intent.getExtras().getInt("profileId");

        return activity;
    }

    private static final byte[] CA_CERTIFICATE = new byte[] {
            (byte) 0x30, (byte) 0x82, (byte) 0x02, (byte) 0x8a, (byte) 0x30, (byte) 0x82,
            (byte) 0x01, (byte) 0xf3, (byte) 0xa0, (byte) 0x03, (byte) 0x02, (byte) 0x01,
            (byte) 0x02, (byte) 0x02, (byte) 0x09, (byte) 0x00, (byte) 0x87, (byte) 0xc0,
            (byte) 0x68, (byte) 0x7f, (byte) 0x42, (byte) 0x92, (byte) 0x0b, (byte) 0x7a,
            (byte) 0x30, (byte) 0x0d, (byte) 0x06, (byte) 0x09, (byte) 0x2a, (byte) 0x86,
            (byte) 0x48, (byte) 0x86, (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x01,
            (byte) 0x05, (byte) 0x05, (byte) 0x00, (byte) 0x30, (byte) 0x5e, (byte) 0x31,
            (byte) 0x0b, (byte) 0x30, (byte) 0x09, (byte) 0x06, (byte) 0x03, (byte) 0x55,
            (byte) 0x04, (byte) 0x06, (byte) 0x13, (byte) 0x02, (byte) 0x41, (byte) 0x55,
            (byte) 0x31, (byte) 0x13, (byte) 0x30, (byte) 0x11, (byte) 0x06, (byte) 0x03,
            (byte) 0x55, (byte) 0x04, (byte) 0x08, (byte) 0x0c, (byte) 0x0a, (byte) 0x53,
            (byte) 0x6f, (byte) 0x6d, (byte) 0x65, (byte) 0x2d, (byte) 0x53, (byte) 0x74,
            (byte) 0x61, (byte) 0x74, (byte) 0x65, (byte) 0x31, (byte) 0x21, (byte) 0x30,
            (byte) 0x1f, (byte) 0x06, (byte) 0x03, (byte) 0x55, (byte) 0x04, (byte) 0x0a,
            (byte) 0x0c, (byte) 0x18, (byte) 0x49, (byte) 0x6e, (byte) 0x74, (byte) 0x65,
            (byte) 0x72, (byte) 0x6e, (byte) 0x65, (byte) 0x74, (byte) 0x20, (byte) 0x57,
            (byte) 0x69, (byte) 0x64, (byte) 0x67, (byte) 0x69, (byte) 0x74, (byte) 0x73,
            (byte) 0x20, (byte) 0x50, (byte) 0x74, (byte) 0x79, (byte) 0x20, (byte) 0x4c,
            (byte) 0x74, (byte) 0x64, (byte) 0x31, (byte) 0x17, (byte) 0x30, (byte) 0x15,
            (byte) 0x06, (byte) 0x03, (byte) 0x55, (byte) 0x04, (byte) 0x03, (byte) 0x0c,
            (byte) 0x0e, (byte) 0x63, (byte) 0x61, (byte) 0x2e, (byte) 0x65, (byte) 0x78,
            (byte) 0x61, (byte) 0x6d, (byte) 0x70, (byte) 0x6c, (byte) 0x65, (byte) 0x2e,
            (byte) 0x63, (byte) 0x6f, (byte) 0x6d, (byte) 0x30, (byte) 0x1e, (byte) 0x17,
            (byte) 0x0d, (byte) 0x31, (byte) 0x33, (byte) 0x30, (byte) 0x38, (byte) 0x32,
            (byte) 0x37, (byte) 0x32, (byte) 0x33, (byte) 0x33, (byte) 0x31, (byte) 0x32,
            (byte) 0x39, (byte) 0x5a, (byte) 0x17, (byte) 0x0d, (byte) 0x32, (byte) 0x33,
            (byte) 0x30, (byte) 0x38, (byte) 0x32, (byte) 0x35, (byte) 0x32, (byte) 0x33,
            (byte) 0x33, (byte) 0x31, (byte) 0x32, (byte) 0x39, (byte) 0x5a, (byte) 0x30,
            (byte) 0x5e, (byte) 0x31, (byte) 0x0b, (byte) 0x30, (byte) 0x09, (byte) 0x06,
            (byte) 0x03, (byte) 0x55, (byte) 0x04, (byte) 0x06, (byte) 0x13, (byte) 0x02,
            (byte) 0x41, (byte) 0x55, (byte) 0x31, (byte) 0x13, (byte) 0x30, (byte) 0x11,
            (byte) 0x06, (byte) 0x03, (byte) 0x55, (byte) 0x04, (byte) 0x08, (byte) 0x0c,
            (byte) 0x0a, (byte) 0x53, (byte) 0x6f, (byte) 0x6d, (byte) 0x65, (byte) 0x2d,
            (byte) 0x53, (byte) 0x74, (byte) 0x61, (byte) 0x74, (byte) 0x65, (byte) 0x31,
            (byte) 0x21, (byte) 0x30, (byte) 0x1f, (byte) 0x06, (byte) 0x03, (byte) 0x55,
            (byte) 0x04, (byte) 0x0a, (byte) 0x0c, (byte) 0x18, (byte) 0x49, (byte) 0x6e,
            (byte) 0x74, (byte) 0x65, (byte) 0x72, (byte) 0x6e, (byte) 0x65, (byte) 0x74,
            (byte) 0x20, (byte) 0x57, (byte) 0x69, (byte) 0x64, (byte) 0x67, (byte) 0x69,
            (byte) 0x74, (byte) 0x73, (byte) 0x20, (byte) 0x50, (byte) 0x74, (byte) 0x79,
            (byte) 0x20, (byte) 0x4c, (byte) 0x74, (byte) 0x64, (byte) 0x31, (byte) 0x17,
            (byte) 0x30, (byte) 0x15, (byte) 0x06, (byte) 0x03, (byte) 0x55, (byte) 0x04,
            (byte) 0x03, (byte) 0x0c, (byte) 0x0e, (byte) 0x63, (byte) 0x61, (byte) 0x2e,
            (byte) 0x65, (byte) 0x78, (byte) 0x61, (byte) 0x6d, (byte) 0x70, (byte) 0x6c,
            (byte) 0x65, (byte) 0x2e, (byte) 0x63, (byte) 0x6f, (byte) 0x6d, (byte) 0x30,
            (byte) 0x81, (byte) 0x9f, (byte) 0x30, (byte) 0x0d, (byte) 0x06, (byte) 0x09,
            (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xf7, (byte) 0x0d,
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x03,
            (byte) 0x81, (byte) 0x8d, (byte) 0x00, (byte) 0x30, (byte) 0x81, (byte) 0x89,
            (byte) 0x02, (byte) 0x81, (byte) 0x81, (byte) 0x00, (byte) 0xa4, (byte) 0xc7,
            (byte) 0x06, (byte) 0xba, (byte) 0xdf, (byte) 0x2b, (byte) 0xee, (byte) 0xd2,
            (byte) 0xb9, (byte) 0xe4, (byte) 0x52, (byte) 0x21, (byte) 0x68, (byte) 0x2b,
            (byte) 0x83, (byte) 0xdf, (byte) 0xe3, (byte) 0x9c, (byte) 0x08, (byte) 0x73,
            (byte) 0xdd, (byte) 0x90, (byte) 0xea, (byte) 0x97, (byte) 0x0c, (byte) 0x96,
            (byte) 0x20, (byte) 0xb1, (byte) 0xee, (byte) 0x11, (byte) 0xd5, (byte) 0xd4,
            (byte) 0x7c, (byte) 0x44, (byte) 0x96, (byte) 0x2e, (byte) 0x6e, (byte) 0xa2,
            (byte) 0xb2, (byte) 0xa3, (byte) 0x4b, (byte) 0x0f, (byte) 0x32, (byte) 0x90,
            (byte) 0xaf, (byte) 0x5c, (byte) 0x6f, (byte) 0x00, (byte) 0x88, (byte) 0x45,
            (byte) 0x4e, (byte) 0x9b, (byte) 0x26, (byte) 0xc1, (byte) 0x94, (byte) 0x3c,
            (byte) 0xfe, (byte) 0x10, (byte) 0xbd, (byte) 0xda, (byte) 0xf2, (byte) 0x8d,
            (byte) 0x03, (byte) 0x52, (byte) 0x32, (byte) 0x11, (byte) 0xff, (byte) 0xf6,
            (byte) 0xf9, (byte) 0x6e, (byte) 0x8f, (byte) 0x0f, (byte) 0xc8, (byte) 0x0a,
            (byte) 0x48, (byte) 0x39, (byte) 0x33, (byte) 0xb9, (byte) 0x0c, (byte) 0xb3,
            (byte) 0x2b, (byte) 0xab, (byte) 0x7d, (byte) 0x79, (byte) 0x6f, (byte) 0x57,
            (byte) 0x5b, (byte) 0xb8, (byte) 0x84, (byte) 0xb6, (byte) 0xcc, (byte) 0xe8,
            (byte) 0x30, (byte) 0x78, (byte) 0xff, (byte) 0x92, (byte) 0xe5, (byte) 0x43,
            (byte) 0x2e, (byte) 0xef, (byte) 0x66, (byte) 0x98, (byte) 0xb4, (byte) 0xfe,
            (byte) 0xa2, (byte) 0x40, (byte) 0xf2, (byte) 0x1f, (byte) 0xd0, (byte) 0x86,
            (byte) 0x16, (byte) 0xc8, (byte) 0x45, (byte) 0xc4, (byte) 0x52, (byte) 0xcb,
            (byte) 0x31, (byte) 0x5c, (byte) 0x9f, (byte) 0x32, (byte) 0x3b, (byte) 0xf7,
            (byte) 0x19, (byte) 0x08, (byte) 0xc7, (byte) 0x00, (byte) 0x21, (byte) 0x7d,
            (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0xa3,
            (byte) 0x50, (byte) 0x30, (byte) 0x4e, (byte) 0x30, (byte) 0x1d, (byte) 0x06,
            (byte) 0x03, (byte) 0x55, (byte) 0x1d, (byte) 0x0e, (byte) 0x04, (byte) 0x16,
            (byte) 0x04, (byte) 0x14, (byte) 0x47, (byte) 0x82, (byte) 0xa3, (byte) 0xf1,
            (byte) 0xc2, (byte) 0x7e, (byte) 0x3a, (byte) 0xde, (byte) 0x4f, (byte) 0x30,
            (byte) 0x4c, (byte) 0x7f, (byte) 0x72, (byte) 0x81, (byte) 0x15, (byte) 0x32,
            (byte) 0xda, (byte) 0x7f, (byte) 0x58, (byte) 0x18, (byte) 0x30, (byte) 0x1f,
            (byte) 0x06, (byte) 0x03, (byte) 0x55, (byte) 0x1d, (byte) 0x23, (byte) 0x04,
            (byte) 0x18, (byte) 0x30, (byte) 0x16, (byte) 0x80, (byte) 0x14, (byte) 0x47,
            (byte) 0x82, (byte) 0xa3, (byte) 0xf1, (byte) 0xc2, (byte) 0x7e, (byte) 0x3a,
            (byte) 0xde, (byte) 0x4f, (byte) 0x30, (byte) 0x4c, (byte) 0x7f, (byte) 0x72,
            (byte) 0x81, (byte) 0x15, (byte) 0x32, (byte) 0xda, (byte) 0x7f, (byte) 0x58,
            (byte) 0x18, (byte) 0x30, (byte) 0x0c, (byte) 0x06, (byte) 0x03, (byte) 0x55,
            (byte) 0x1d, (byte) 0x13, (byte) 0x04, (byte) 0x05, (byte) 0x30, (byte) 0x03,
            (byte) 0x01, (byte) 0x01, (byte) 0xff, (byte) 0x30, (byte) 0x0d, (byte) 0x06,
            (byte) 0x09, (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xf7,
            (byte) 0x0d, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x05, (byte) 0x00,
            (byte) 0x03, (byte) 0x81, (byte) 0x81, (byte) 0x00, (byte) 0x08, (byte) 0x7f,
            (byte) 0x6a, (byte) 0x48, (byte) 0x90, (byte) 0x7b, (byte) 0x9b, (byte) 0x72,
            (byte) 0x13, (byte) 0xa7, (byte) 0xef, (byte) 0x6b, (byte) 0x0b, (byte) 0x59,
            (byte) 0xe5, (byte) 0x49, (byte) 0x72, (byte) 0x3a, (byte) 0xc8, (byte) 0x84,
            (byte) 0xcc, (byte) 0x23, (byte) 0x18, (byte) 0x4c, (byte) 0xec, (byte) 0xc7,
            (byte) 0xef, (byte) 0xcb, (byte) 0xa7, (byte) 0xbe, (byte) 0xe4, (byte) 0xef,
            (byte) 0x8f, (byte) 0xc6, (byte) 0x06, (byte) 0x8c, (byte) 0xc0, (byte) 0xe4,
            (byte) 0x2f, (byte) 0x2a, (byte) 0xc0, (byte) 0x35, (byte) 0x7d, (byte) 0x5e,
            (byte) 0x19, (byte) 0x29, (byte) 0x8c, (byte) 0xb9, (byte) 0xf1, (byte) 0x1e,
            (byte) 0xaf, (byte) 0x82, (byte) 0xd8, (byte) 0xe3, (byte) 0x88, (byte) 0xe1,
            (byte) 0x31, (byte) 0xc8, (byte) 0x82, (byte) 0x1f, (byte) 0x83, (byte) 0xa9,
            (byte) 0xde, (byte) 0xfe, (byte) 0x4b, (byte) 0xe2, (byte) 0x78, (byte) 0x64,
            (byte) 0xed, (byte) 0xa4, (byte) 0x7b, (byte) 0xee, (byte) 0x8d, (byte) 0x71,
            (byte) 0x1b, (byte) 0x44, (byte) 0xe6, (byte) 0xb7, (byte) 0xe8, (byte) 0xc5,
            (byte) 0x9a, (byte) 0x93, (byte) 0x92, (byte) 0x6f, (byte) 0x6f, (byte) 0xdb,
            (byte) 0xbd, (byte) 0xd7, (byte) 0x03, (byte) 0x85, (byte) 0xa9, (byte) 0x5f,
            (byte) 0x53, (byte) 0x5f, (byte) 0x5d, (byte) 0x30, (byte) 0xc6, (byte) 0xd9,
            (byte) 0xce, (byte) 0x34, (byte) 0xa8, (byte) 0xbe, (byte) 0x31, (byte) 0x47,
            (byte) 0x1c, (byte) 0xa4, (byte) 0x7f, (byte) 0xc0, (byte) 0x2c, (byte) 0xbc,
            (byte) 0xfe, (byte) 0x1a, (byte) 0x31, (byte) 0xd8, (byte) 0x77, (byte) 0x4d,
            (byte) 0xfc, (byte) 0x45, (byte) 0x84, (byte) 0xfc, (byte) 0x45, (byte) 0x12,
            (byte) 0xab, (byte) 0x50, (byte) 0xe4, (byte) 0x45, (byte) 0xe5, (byte) 0x11
    };

}

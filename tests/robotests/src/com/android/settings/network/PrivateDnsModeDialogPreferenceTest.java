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
 */

package com.android.settings.network;

import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PREDEFINED_PROVIDER;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static android.net.ConnectivityManager.PRIVATE_DNS_VALIDATION_RESULT_FAIL;
import static android.net.ConnectivityManager.PRIVATE_DNS_VALIDATION_RESULT_SUCCESS;
import static android.net.ConnectivityManager.ValidationCallback;
import static android.provider.Settings.Global.PRIVATE_DNS_MODE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.PrivateDnsProvider;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowOs;
import com.android.settingslib.CustomDialogPreferenceCompat.CustomPreferenceDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowOs.class)
public class PrivateDnsModeDialogPreferenceTest {

    private static final String HOST_NAME = "dns.example.com";
    private static final String INVALID_HOST_NAME = "...,";

    private static final String VALID_URL = "https://dns.example/dns-query";
    private static final String INVALID_URL1 = "http://dns.example/dns-query";
    private static final String INVALID_URL2 = "https://";

    private static final List<PrivateDnsProvider> PROVIDER_LIST =
            Arrays.asList(new PrivateDnsProvider("Google"), new PrivateDnsProvider("Cloudflare"));
    private static final String PROVIDER_NAME = "Google";
    private static final String PROVIDER_NAME_NOT_IN_LIST = "OpenDNS";
    private PrivateDnsModeDialogPreference mPreference;

    private Context mContext;
    private Button mPositiveButton;
    private Button mNegativeButton;

    @Mock
    private ConnectivityManager mConnectivityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ReflectionHelpers.setStaticField(android.system.OsConstants.class, "AF_INET", 2);
        ReflectionHelpers.setStaticField(android.system.OsConstants.class, "AF_INET6", 10);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        doReturn(PROVIDER_LIST).when(mConnectivityManager).getPrivateDnsProviders();

        mPositiveButton = new Button(mContext);
        mNegativeButton = new Button(mContext);

        final CustomPreferenceDialogFragment fragment = mock(CustomPreferenceDialogFragment.class);
        final AlertDialog dialog = mock(AlertDialog.class);
        when(fragment.getDialog()).thenReturn(dialog);
        when(dialog.getButton(eq(DialogInterface.BUTTON_POSITIVE))).thenReturn(mPositiveButton);
        when(dialog.getButton(eq(DialogInterface.BUTTON_NEGATIVE))).thenReturn(mNegativeButton);

        mPreference = new PrivateDnsModeDialogPreference(mContext);
        ReflectionHelpers.setField(mPreference, "mFragment", fragment);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.private_dns_mode_dialog,
                new LinearLayout(mContext), false);

        mPreference.onBindDialogView(view);
    }

    @Test
    public void testOnCheckedChanged_dnsModeOff_disableOthers() {
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_off);

        assertThat(mPreference.mMode).isEqualTo(PRIVATE_DNS_MODE_OFF);
        assertThat(mPreference.mEditText.isEnabled()).isFalse();
        assertThat(mPreference.mSp.isEnabled()).isFalse();
    }

    @Test
    public void testOnCheckedChanged_dnsModeOpportunistic_disableOthers() {
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_opportunistic);

        assertThat(mPreference.mMode).isEqualTo(PRIVATE_DNS_MODE_OPPORTUNISTIC);
        assertThat(mPreference.mEditText.isEnabled()).isFalse();
        assertThat(mPreference.mSp.isEnabled()).isFalse();
    }

    @Test
    public void testOnCheckedChanged_dnsModeProvider_enableEditText() {
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_provider);

        assertThat(mPreference.mMode).isEqualTo(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        assertThat(mPreference.mEditText.isEnabled()).isTrue();
    }

    @Test
    public void testOnCheckedChanged_dnsModePreDefinedProvider_enableSpinner() {
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_predefined_provider);

        assertThat(mPreference.mMode).isEqualTo(PRIVATE_DNS_MODE_PREDEFINED_PROVIDER);
        assertThat(mPreference.mSp.isEnabled()).isTrue();
    }

    @Test
    public void testOnBindDialogView_containsCorrectData() {
        // Don't set settings to the default value ("opportunistic") as that
        // risks masking failure to read the mode from settings.
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogPreference.MODE_KEY, PRIVATE_DNS_MODE_OFF);
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogPreference.CUSTOMIZATION_KEY, HOST_NAME);
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogPreference.PREDEFINED_PROVIDER_KEY, PROVIDER_NAME);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.private_dns_mode_dialog,
                new LinearLayout(mContext), false);
        mPreference.onBindDialogView(view);

        assertThat(mPreference.mEditText.getText().toString()).isEqualTo(HOST_NAME);
        assertThat(mPreference.mSp.getSelectedItem().toString()).isEqualTo(PROVIDER_NAME);
        assertThat(mPreference.mRadioGroup.getCheckedRadioButtonId()).isEqualTo(
                R.id.private_dns_mode_off);

        final Adapter adapter = mPreference.mSp.getAdapter();
        int count = adapter.getCount();
        final List<PrivateDnsProvider> providers = new ArrayList<PrivateDnsProvider>(count);
        for (int i = 0; i < count; i++) {
            providers.add(new PrivateDnsProvider((String) adapter.getItem(i)));
        }
        assertThat(providers).isEqualTo(PROVIDER_LIST);

        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogPreference.PREDEFINED_PROVIDER_KEY, PROVIDER_NAME_NOT_IN_LIST);
        mPreference.onBindDialogView(view);
        assertThat(
                mPreference.mSp.getSelectedItem().toString()).isEqualTo(PROVIDER_NAME_NOT_IN_LIST);

        reset(mConnectivityManager);
        doReturn(List.of()).when(mConnectivityManager).getPrivateDnsProviders();
        mPreference.onBindDialogView(view);
        assertThat(
                mPreference.mSp.getSelectedItem().toString()).isEqualTo(PROVIDER_NAME_NOT_IN_LIST);
    }

    @Test
    public void testOnBindDialogView_spinnerHasCorrectState() {
        // Don't set settings to the default value ("opportunistic") as that
        // risks masking failure to read the mode from settings.
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogPreference.MODE_KEY, PRIVATE_DNS_MODE_OFF);
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogPreference.PREDEFINED_PROVIDER_KEY, "");

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.private_dns_mode_dialog,
                new LinearLayout(mContext), false);
        mPreference.onBindDialogView(view);

        assertThat(mPreference.mSp.getVisibility()).isEqualTo(View.VISIBLE);

        reset(mConnectivityManager);
        doReturn(List.of()).when(mConnectivityManager).getPrivateDnsProviders();
        final View view2 = inflater.inflate(R.layout.private_dns_mode_dialog,
                new LinearLayout(mContext), false);
        mPreference.onBindDialogView(view2);
        assertThat(mPreference.mSp.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testOnCheckedChanged_switchMode_saveButtonHasCorrectState() {
        final String[] invalidInputs = new String[] {
                INVALID_HOST_NAME,
                INVALID_URL1,
                INVALID_URL2,
                "2001:db8::53",  // IPv6 string literal
                "192.168.1.1",   // IPv4 string literal
        };

        for (String invalid : invalidInputs) {
            // Set invalid input
            mPreference.mEditText.setText(invalid);

            mPreference.onCheckedChanged(null, R.id.private_dns_mode_off);
            assertWithMessage("off: " + invalid).that(mPositiveButton.isEnabled()).isTrue();

            mPreference.onCheckedChanged(null, R.id.private_dns_mode_opportunistic);
            assertWithMessage(
                    "opportunistic: " + invalid).that(mPositiveButton.isEnabled()).isTrue();

            mPreference.onCheckedChanged(null, R.id.private_dns_mode_provider);
            assertWithMessage("provider: " + invalid).that(mPositiveButton.isEnabled()).isFalse();
        }

        final String[] validInputs = new String[] {
            HOST_NAME,
            VALID_URL,
        };
        for (String valid : validInputs) {
            // Set valid input
            mPreference.mEditText.setText(valid);

            mPreference.onCheckedChanged(null, R.id.private_dns_mode_provider);
            assertWithMessage("provider: " + valid).that(mPositiveButton.isEnabled()).isTrue();
        }
    }

    @Test
    public void testOnClick_positiveButtonClicked_saveData() {
        // Set the default settings to OFF
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, PRIVATE_DNS_MODE_OFF);

        mPreference.mMode = PRIVATE_DNS_MODE_OPPORTUNISTIC;
        final AlertDialog dialog = mock(AlertDialog.class);
        mPreference.onShow(dialog);
        mPositiveButton.performClick();

        verify(dialog, times(1)).dismiss();
        // Change to OPPORTUNISTIC
        assertThat(Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)).isEqualTo(
                PRIVATE_DNS_MODE_OPPORTUNISTIC);
    }

    @Test
    public void testOnClick_negativeButtonClicked_doNothing() {
        // Set the default settings to OFF
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, PRIVATE_DNS_MODE_OFF);

        mPreference.mMode = PRIVATE_DNS_MODE_OPPORTUNISTIC;
        mNegativeButton.performClick();

        // Still equal to OFF
        assertThat(Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)).isEqualTo(
                PRIVATE_DNS_MODE_OFF);
    }

    @Test
    public void testOnClick_positiveButtonClicked_validation_success() {
        // Set the default settings to OFF
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, PRIVATE_DNS_MODE_OFF);

        mPreference.mMode = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        mPreference.mEditText.setText(HOST_NAME);
        final AlertDialog dialog = mock(AlertDialog.class);
        mPreference.onShow(dialog);
        mPositiveButton.performClick();

        final ArgumentCaptor<ValidationCallback> callbackCaptor =
                ArgumentCaptor.forClass(ValidationCallback.class);
        verify(mConnectivityManager, times(1))
                .validatePrivateDnsSetting(eq(HOST_NAME), any(), callbackCaptor.capture());
        final ValidationCallback cb = callbackCaptor.getValue();
        cb.onResult(PRIVATE_DNS_VALIDATION_RESULT_SUCCESS);

        verify(dialog, times(1)).dismiss();
        // Change to HOSTNAME
        assertThat(Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)).isEqualTo(
                PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        assertThat(Settings.Global.getString(contentResolver,
                PrivateDnsModeDialogPreference.CUSTOMIZATION_KEY)).isEqualTo(HOST_NAME);
    }

    private void assertComponentsHaveCorrectState(boolean enabled) {
        assertEquals(mPreference.mProgressBar.getVisibility(), enabled ? View.GONE : View.VISIBLE);
        for (int i = 0; i < mPreference.mRadioGroup.getChildCount(); i++) {
            assertEquals(mPreference.mRadioGroup.getChildAt(i).isEnabled(), enabled);
        }
        assertEquals(mPositiveButton.isEnabled(), enabled);
    }

    @Test
    public void testOnClick_positiveButtonClicked_validation_fail() {
        // Set the default settings to OFF
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, PRIVATE_DNS_MODE_OFF);

        mPreference.mMode = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        mPreference.mEditText.setText(INVALID_HOST_NAME);
        final AlertDialog dialog = mock(AlertDialog.class);
        mPreference.onShow(dialog);
        mPositiveButton.performClick();

        final ArgumentCaptor<ValidationCallback> callbackCaptor =
                ArgumentCaptor.forClass(ValidationCallback.class);
        verify(mConnectivityManager, times(1)).validatePrivateDnsSetting(
                eq(INVALID_HOST_NAME), any(), callbackCaptor.capture());

        // Before validation result is received, private DNS UI components should be disabled.
        assertComponentsHaveCorrectState(false);

        final ValidationCallback cb = callbackCaptor.getValue();
        cb.onResult(PRIVATE_DNS_VALIDATION_RESULT_FAIL);
        // After validation result is received, private DNS UI components should be enabled.
        assertComponentsHaveCorrectState(true);

        // dialog still exists because validation failed.
        verify(dialog, never()).dismiss();
        // Still equal to OFF
        assertThat(Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)).isEqualTo(
                PRIVATE_DNS_MODE_OFF);
    }
}

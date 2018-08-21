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
 * limitations under the License
 */
package com.android.settings.network;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.shadow.api.Shadow.extract;

import androidx.lifecycle.LifecycleOwner;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {
        ShadowConnectivityManager.class,
        ShadowUserManager.class}
)
public class MobileNetworkPreferenceControllerTest {

    private static final int SUBID_1 = 1;
    private static final int SUBID_2 = 2;
    private static final String TEST_CARRIER_NAME1 = "test1";
    private static final String TEST_CARRIER_NAME2 = "test2";
    private Context mContext;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private PreferenceScreen mScreen;

    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private MobileNetworkPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE))
                .thenReturn(mSubscriptionManager);

        List<SubscriptionInfo> list = new ArrayList<SubscriptionInfo>();
        list.add(new SubscriptionInfo(SUBID_1, "", 0, "", "",
                0, 0, "", 0, null, "0", "0", "", false, null, null));
        list.add(new SubscriptionInfo(SUBID_2, "", 1, "", "",
                0, 0, "", 0, null, "0", "0", "", false, null, null));
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(list);

        when(mTelephonyManager.getNetworkOperatorName(eq(SUBID_1))).thenReturn(TEST_CARRIER_NAME1);
        when(mTelephonyManager.getNetworkOperatorName(eq(SUBID_2))).thenReturn(TEST_CARRIER_NAME2);
    }

    @Test
    public void secondaryUser_prefIsNotAvailable() {
        ShadowUserManager userManager = extract(mContext.getSystemService(UserManager.class));
        userManager.setIsAdminUser(false);
        ShadowConnectivityManager connectivityManager =
                extract(mContext.getSystemService(ConnectivityManager.class));
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, true);

        mController = new MobileNetworkPreferenceController(mContext);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void wifiOnly_prefIsNotAvailable() {
        ShadowUserManager userManager = extract(mContext.getSystemService(UserManager.class));
        userManager.setIsAdminUser(true);
        ShadowConnectivityManager connectivityManager =
                extract(mContext.getSystemService(ConnectivityManager.class));
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, false);

        mController = new MobileNetworkPreferenceController(mContext);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void goThroughLifecycle_isAvailable_shouldListenToServiceChange() {
        mController = spy(new MobileNetworkPreferenceController(mContext));
        mLifecycle.addObserver(mController);
        doReturn(true).when(mController).isAvailable();

        mLifecycle.handleLifecycleEvent(ON_START);
        verify(mTelephonyManager).listen(mController.mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE);

        mLifecycle.handleLifecycleEvent(ON_STOP);
        verify(mTelephonyManager).listen(mController.mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
    }

    @Test
    public void serviceStateChange_shouldUpdatePrefSummary() {
        final Preference mPreference = mock(Preference.class);
        mController = spy(new MobileNetworkPreferenceController(mContext));
        mLifecycle.addObserver(mController);

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        doReturn(true).when(mController).isAvailable();

        // Display pref and go through lifecycle to set up listener.
        mController.displayPreference(mScreen);
        mLifecycle.handleLifecycleEvent(ON_START);
        verify(mController).onStart();
        verify(mTelephonyManager).listen(mController.mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE);
        verify(mSubscriptionManager).addOnSubscriptionsChangedListener(
                mController.mOnSubscriptionsChangeListener);
        doReturn(true).when(mController).isSubscriptionInService(anyInt());

        // Trigger listener update
        mController.mPhoneStateListener.onServiceStateChanged(null);

        // Carrier name should be set.
        verify(mPreference).setSummary(TEST_CARRIER_NAME1);
    }

    @Test
    public void serviceStateChange_shouldUpdatePrefSummaryForMultiSim() {
        final Preference mPreference = mock(Preference.class);
        final String testCarrierName = TEST_CARRIER_NAME1 + ", " + TEST_CARRIER_NAME2;
        mController = spy(new MobileNetworkPreferenceController(mContext));
        mLifecycle.addObserver(mController);

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        doReturn(true).when(mController).isAvailable();

        // Display pref and go through lifecycle to set up listener.
        mController.displayPreference(mScreen);
        mLifecycle.handleLifecycleEvent(ON_START);
        verify(mController).onStart();
        verify(mTelephonyManager).listen(mController.mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE);
        verify(mSubscriptionManager).addOnSubscriptionsChangedListener(
                mController.mOnSubscriptionsChangeListener);
        doReturn(true).when(mController).isSubscriptionInService(anyInt());

        mController.mPhoneStateListener.onServiceStateChanged(null);

        // Carrier name should be set.
        verify(mPreference).setSummary(testCarrierName);
    }

    @Test
    public void airplaneModeTurnedOn_shouldDisablePreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Global.AIRPLANE_MODE_ON, 1);
        mController = spy(new MobileNetworkPreferenceController(mContext));
        final RestrictedPreference mPreference = new RestrictedPreference(mContext);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void airplaneModeTurnedOffAndNoUserRestriction_shouldEnablePreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Global.AIRPLANE_MODE_ON, 0);
        mController = spy(new MobileNetworkPreferenceController(mContext));
        final RestrictedPreference mPreference = new RestrictedPreference(mContext);
        mPreference.setDisabledByAdmin(null);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void airplaneModeTurnedOffAndHasUserRestriction_shouldDisablePreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Global.AIRPLANE_MODE_ON, 0);
        mController = spy(new MobileNetworkPreferenceController(mContext));
        final RestrictedPreference mPreference = new RestrictedPreference(mContext);
        mPreference.setDisabledByAdmin(EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
    }
}

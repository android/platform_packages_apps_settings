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
 * limitations under the License
 */

package com.android.settings.wifi.calling;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.network.ims.ImsQuery;
import com.android.settings.network.ims.ImsQueryResultTest;
import com.android.settings.widget.RtlCompatibleViewPager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowSubscriptionManager;
import org.robolectric.shadows.ShadowSubscriptionManager.SubscriptionInfoBuilder;
import org.robolectric.shadows.androidx.fragment.FragmentController;


@RunWith(RobolectricTestRunner.class)
public class WifiCallingSettingsTest {

    private Context mContext;
    private ShadowContextImpl mShadowContextImpl;

    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mCarrierConfig;

    private ImsQuery mImsQueryTrue;
    private ImsQuery mImsQueryFalse;

    private static final int SUB_ID_111 = 111;
    private static final int SUB_ID_222 = 222;

    private SubscriptionManager mSubscriptionManager;
    private ShadowSubscriptionManager mShadowSubscriptionManager;
    private SubscriptionInfo mSubscriptionInfo1;
    private SubscriptionInfo mSubscriptionInfo2;

    private WifiCallingSettings mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application.getBaseContext();
        mShadowContextImpl = Shadow.extract(mContext);

        mCarrierConfig = new PersistableBundle();
        mShadowContextImpl.setSystemService(Context.CARRIER_CONFIG_SERVICE, mCarrierConfigManager);
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(anyInt());

        mImsQueryTrue = new ImsQueryResultTest.ImsQueryBoolean(true);
        mImsQueryFalse = new ImsQueryResultTest.ImsQueryBoolean(false);

        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mShadowSubscriptionManager = shadowOf(mSubscriptionManager);
        mSubscriptionInfo1 = SubscriptionInfoBuilder.newBuilder()
                .setId(SUB_ID_111).buildSubscriptionInfo();
        mSubscriptionInfo2 = SubscriptionInfoBuilder.newBuilder()
                .setId(SUB_ID_222).buildSubscriptionInfo();

    }

    private WifiCallingSettings createFragment(Intent intent) {
        return spy(new WifiCallingSettings() {
            @Override
            Intent getActivityIntent() {
                return intent;
            }
        });
    }

    @Test
    public void setupFragment_noSubscriptions_noCrash() {
        mFragment = createFragment(null);
        FragmentController.setupFragment(mFragment, FragmentActivity.class, 0 /* containerViewId*/,
                null /* bundle */);
    }

    @Test
    public void setupFragment_oneSubscription_noCrash() {
        mShadowSubscriptionManager.setActiveSubscriptionInfos(mSubscriptionInfo1);

        final Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_SUB_ID, SUB_ID_111);
        mFragment = createFragment(intent);

        doReturn(mImsQueryTrue).when(mFragment).isWfcEnabledByPlatform(anyInt());
        doReturn(mImsQueryTrue).when(mFragment).isWfcProvisionedOnDevice(anyInt());

        FragmentController.of(mFragment, intent).create(0 /* containerViewId*/,
                null /* bundle */).start().resume().visible().get();

        final View view = mFragment.getView();
        final RtlCompatibleViewPager pager = view.findViewById(R.id.view_pager);
        final WifiCallingSettings.WifiCallingViewPagerAdapter adapter =
                (WifiCallingSettings.WifiCallingViewPagerAdapter) pager.getAdapter();
        assertThat(adapter.getCount()).isEqualTo(1);
    }

    @Test
    public void setupFragment_twoSubscriptions_correctSelection() {
        mShadowSubscriptionManager.setActiveSubscriptionInfos(
                mSubscriptionInfo1, mSubscriptionInfo2);

        final Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_SUB_ID, SUB_ID_222);
        mFragment = createFragment(intent);

        doReturn(mImsQueryTrue).when(mFragment).isWfcEnabledByPlatform(anyInt());
        doReturn(mImsQueryTrue).when(mFragment).isWfcProvisionedOnDevice(anyInt());

        FragmentController.of(mFragment, intent).create(0 /* containerViewId*/,
                null /* bundle */).start().resume().visible().get();

        final View view = mFragment.getView();
        final RtlCompatibleViewPager pager = view.findViewById(R.id.view_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(1);

        final WifiCallingSettings.WifiCallingViewPagerAdapter adapter =
                (WifiCallingSettings.WifiCallingViewPagerAdapter) pager.getAdapter();
        assertThat(adapter.getCount()).isEqualTo(2);
    }

    @Test
    public void setupFragment_twoSubscriptionsOneNotProvisionedOnDevice_oneResult() {
        mShadowSubscriptionManager.setActiveSubscriptionInfos(
                mSubscriptionInfo1, mSubscriptionInfo2);

        final Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_SUB_ID, SUB_ID_111);
        mFragment = createFragment(intent);

        doReturn(mImsQueryTrue).when(mFragment).isWfcEnabledByPlatform(anyInt());
        doReturn(mImsQueryTrue).when(mFragment).isWfcProvisionedOnDevice(SUB_ID_111);
        doReturn(mImsQueryFalse).when(mFragment).isWfcProvisionedOnDevice(SUB_ID_222);

        FragmentController.of(mFragment, intent).create(0 /* containerViewId*/,
                null /* bundle */).start().resume().visible().get();

        final View view = mFragment.getView();
        final RtlCompatibleViewPager pager = view.findViewById(R.id.view_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(0);

        final WifiCallingSettings.WifiCallingViewPagerAdapter adapter =
                (WifiCallingSettings.WifiCallingViewPagerAdapter) pager.getAdapter();
        assertThat(adapter.getCount()).isEqualTo(1);
    }
}

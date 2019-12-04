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

package com.android.settings.network.telephony;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.network.ims.ImsQuery;
import com.android.settings.network.ims.ImsQueryResultTest;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SettingsSliceProvider;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SlicesFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowSubscriptionManager;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class Enhanced4gLteSliceHelperTest {
    private static final int SUB_ID = 1;

    private Context mContext;
    private ShadowContextImpl mShadowContextImpl;

    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mCarrierConfig;

    private ShadowSubscriptionManager mShadowSubscriptionManager;

    private ImsQuery mImsQueryTrue;
    private ImsQuery mImsQueryFalse;

    private FakeFeatureFactory mFeatureFactory;
    private SlicesFeatureProvider mSlicesFeatureProvider;
    private SettingsSliceProvider mProvider;
    private SliceBroadcastReceiver mReceiver;

    private Enhanced4gLteSliceHelper mEnhanced4gLteSliceHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application.getBaseContext();
        mShadowContextImpl = Shadow.extract(mContext);

        mCarrierConfig = new PersistableBundle();
        mShadowContextImpl.setSystemService(Context.CARRIER_CONFIG_SERVICE, mCarrierConfigManager);
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(anyInt());
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true);

        mShadowSubscriptionManager = shadowOf(
                mContext.getSystemService(SubscriptionManager.class));
        mShadowSubscriptionManager.setDefaultVoiceSubscriptionId(SUB_ID);

        mImsQueryTrue = new ImsQueryResultTest.ImsQueryBoolean(true);
        mImsQueryFalse = new ImsQueryResultTest.ImsQueryBoolean(false);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSlicesFeatureProvider = mFeatureFactory.getSlicesFeatureProvider();

        //setup for SettingsSliceProvider tests
        mProvider = spy(new SettingsSliceProvider());
        doReturn(mContext).when(mProvider).getContext();
        mProvider.onCreateSliceProvider();

        //setup for SliceBroadcastReceiver test
        mReceiver = spy(new SliceBroadcastReceiver());

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mEnhanced4gLteSliceHelper = spy(new Enhanced4gLteSliceHelper(mContext));
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper).isSystemTtyEnabled();
    }

    @Test
    public void test_CreateEnhanced4gLteSlice_invalidSubId() {
        mShadowSubscriptionManager.setDefaultVoiceSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isVolteProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isVolteEnabledByPlatform(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isEnhanced4gLteModeSettingEnabledByUser(anyInt());

        final Slice slice = mEnhanced4gLteSliceHelper.createEnhanced4gLteSlice(
                CustomSliceRegistry.ENHANCED_4G_SLICE_URI);

        assertThat(slice).isNull();
    }

    @Test
    public void test_CreateEnhanced4gLteSlice_enhanced4gLteNotSupported() {
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryFalse).when(mEnhanced4gLteSliceHelper)
                .isVolteProvisionedOnDevice(anyInt());
        doReturn(mImsQueryFalse).when(mEnhanced4gLteSliceHelper)
                .isVolteEnabledByPlatform(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isEnhanced4gLteModeSettingEnabledByUser(anyInt());

        final Slice slice = mEnhanced4gLteSliceHelper.createEnhanced4gLteSlice(
                CustomSliceRegistry.ENHANCED_4G_SLICE_URI);

        assertThat(slice).isNull();
    }

    @Test
    public void test_CreateEnhanced4gLteSlice_success() {
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isVolteProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isVolteEnabledByPlatform(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isEnhanced4gLteModeSettingEnabledByUser(anyInt());

        final Slice slice = mEnhanced4gLteSliceHelper.createEnhanced4gLteSlice(
                CustomSliceRegistry.ENHANCED_4G_SLICE_URI);

        testEnhanced4gLteSettingsToggleSlice(slice);
    }

    @Test
    public void test_SettingSliceProvider_getsRightSliceEnhanced4gLte() {
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isVolteProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isVolteEnabledByPlatform(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isEnhanced4gLteModeSettingEnabledByUser(anyInt());

        when(mSlicesFeatureProvider.getNewEnhanced4gLteSliceHelper(mContext))
                .thenReturn(mEnhanced4gLteSliceHelper);

        final Slice slice = mProvider.onBindSlice(CustomSliceRegistry.ENHANCED_4G_SLICE_URI);

        assertThat(mEnhanced4gLteSliceHelper.getDefaultVoiceSubId()).isEqualTo(SUB_ID);
        testEnhanced4gLteSettingsToggleSlice(slice);
    }

    @Test
    public void test_SliceBroadcastReceiver_toggleOffEnhanced4gLte() {
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isTtyOnVolteEnabled(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isVolteProvisionedOnDevice(anyInt());
        doReturn(mImsQueryTrue).when(mEnhanced4gLteSliceHelper)
                .isVolteEnabledByPlatform(anyInt());
        doReturn(mImsQueryFalse).when(mEnhanced4gLteSliceHelper)
                .isEnhanced4gLteModeSettingEnabledByUser(anyInt());

        when(mSlicesFeatureProvider.getNewEnhanced4gLteSliceHelper(mContext))
                .thenReturn(mEnhanced4gLteSliceHelper);

        final ArgumentCaptor<Boolean> mEnhanced4gLteSettingCaptor = ArgumentCaptor.forClass(
                Boolean.class);

        doNothing().when(mEnhanced4gLteSliceHelper).setEnhanced4gLteModeSetting(anyInt(),
                mEnhanced4gLteSettingCaptor.capture());

        // turn on Enhanced4gLte setting
        final Intent intent = new Intent(Enhanced4gLteSliceHelper.ACTION_ENHANCED_4G_LTE_CHANGED);
        intent.putExtra(EXTRA_TOGGLE_STATE, true);

        // change the setting
        mReceiver.onReceive(mContext, intent);

        // assert the change
        assertThat(mEnhanced4gLteSettingCaptor.getValue()).isTrue();
    }

    private void testEnhanced4gLteSettingsToggleSlice(Slice slice) {
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction mainToggleAction = toggles.get(0);

        // Check intent in Toggle Action
        final PendingIntent togglePendingIntent = mainToggleAction.getAction();
        final PendingIntent expectedToggleIntent = getBroadcastIntent(
                Enhanced4gLteSliceHelper.ACTION_ENHANCED_4G_LTE_CHANGED);
        assertThat(togglePendingIntent).isEqualTo(expectedToggleIntent);

        // Check primary intent
        final PendingIntent primaryPendingIntent = metadata.getPrimaryAction().getAction();
        final PendingIntent expectedPendingIntent =
                getActivityIntent(Enhanced4gLteSliceHelper.ACTION_MOBILE_NETWORK_SETTINGS_ACTIVITY);
        assertThat(primaryPendingIntent).isEqualTo(expectedPendingIntent);

        // Check the title
        final List<SliceItem> sliceItems = slice.getItems();
        assertTitle(sliceItems, mContext.getString(R.string.enhanced_4g_lte_mode_title));
    }

    private PendingIntent getBroadcastIntent(String action) {
        final Intent intent = new Intent(action);
        intent.setClass(mContext, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(mContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getActivityIntent(String action) {
        final Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(mContext, 0 /* requestCode */, intent, 0 /* flags */);
    }

    private void assertTitle(List<SliceItem> sliceItems, String title) {
        boolean hasTitle = false;
        for (SliceItem item : sliceItems) {
            final List<SliceItem> titleItems = SliceQuery.findAll(item, FORMAT_TEXT, HINT_TITLE,
                    null /* non-hints */);
            if (titleItems == null) {
                continue;
            }

            hasTitle = true;
            for (SliceItem subTitleItem : titleItems) {
                assertThat(subTitleItem.getText()).isEqualTo(title);
            }
        }
        assertThat(hasTitle).isTrue();
    }
}

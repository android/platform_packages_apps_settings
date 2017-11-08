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

package com.android.settings.wfc;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowDisclaimerItemFactory;
import com.android.settings.WifiCallingSettings;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ActivityController;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowDisclaimerItemFactory.class
        })
public class DisclaimerActivityTest {
    @Mock
    LayoutInflater mMockInflater;
    @Mock
    View mMockView;
    @Mock
    LinearLayout mMockLinearLayout;
    @Mock
    ListView mMockDisclaimerItemListView;
    @Mock
    DisclaimerItem mMockDisclaimerItem;
    @Mock
    AbsListView mMockAbsListView;
    @Mock
    Button mMockAgreeButton;

    @Captor
    ArgumentCaptor<DisclaimerActivity.DisclaimerItemListAdapter> mDisclaimerItemListAdapterCaptor;
    @Captor
    ArgumentCaptor<OnScrollListener> mOnScrollListenerCaptor;
    @Captor
    ArgumentCaptor<OnClickListener> mAgreeOnClickListenerCaptor;

    private Intent mLaunchIntent = new Intent().putExtra(WifiCallingSettings.EXTRA_PHONE_ID, 0);
    private ActivityController<DisclaimerActivity> mActivityController;
    private DisclaimerActivity mActivity;
    private Application mApplication;
    private List<DisclaimerItem> mDisclaimerItemList = new ArrayList<>();
    private List<DisclaimerItem> mBlankDisclaimerItemList = new ArrayList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mApplication = RuntimeEnvironment.application;
        mActivityController = Robolectric.buildActivity(DisclaimerActivity.class);
        mActivity = spy(mActivityController.get());

        doReturn(mLaunchIntent).when(mActivity).getIntent();
        doReturn(mMockInflater).when(mActivity).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        doReturn(mMockDisclaimerItemListView).when(mActivity).findViewById(
                R.id.disclamer_item_list);
        doReturn(mMockAgreeButton).when(mActivity).findViewById(R.id.agree_button);

        doNothing().when(mMockDisclaimerItemListView).setAdapter(
                mDisclaimerItemListAdapterCaptor.capture());
        doNothing().when(mMockDisclaimerItemListView).setOnScrollListener(
                mOnScrollListenerCaptor.capture());
        doNothing().when(mMockAgreeButton).setOnClickListener(
                mAgreeOnClickListenerCaptor.capture());

        when(mMockInflater.inflate(anyInt(), anyObject(), anyBoolean())).thenReturn(mMockView);
        when(mMockView.findViewById(R.id.disclaimer_item_container)).thenReturn(mMockLinearLayout);

        mDisclaimerItemList.add(mMockDisclaimerItem);
    }

    @Test
    public void onCreate_notHaveItem_shouldFinishActivity() {
        ShadowDisclaimerItemFactory.setDisclaimerItemList(mBlankDisclaimerItemList);

        mActivity.onCreate(null);

        verify(mActivity).setResult(Activity.RESULT_OK, null);
        verify(mActivity).finish();
    }

    @Test
    public void onCreate_haveItem_shouldShowDisclaimerActivityScreen() {
        ShadowDisclaimerItemFactory.setDisclaimerItemList(mDisclaimerItemList);

        mActivity.onCreate(null);

        // Check the WFC disclaimer activity screen is shown.
        verify(mActivity, never()).setResult(anyInt(), anyObject());
        verify(mActivity, never()).finish();
        verify(mActivity).setContentView(R.layout.wfc_disclaimer_activity);
        // Check the WFC disclaimer item is setted to the disclaimer list adapter.
        assertEquals(mDisclaimerItemListAdapterCaptor.getValue().getCount(), 1);
    }

    @Test
    public void onScroll_canNotScroll_shouldEnableAgreeButton() {
        ShadowDisclaimerItemFactory.setDisclaimerItemList(mDisclaimerItemList);
        when(mMockAbsListView.canScrollVertically(1)).thenReturn(false);

        mActivity.onCreate(null);
        mOnScrollListenerCaptor.getValue().onScroll(mMockAbsListView, 0, 0, 0);

        // Check the agreeButton is enabled when scrolled to bottom end.
        verify(mMockAgreeButton).setEnabled(true);
    }

    @Test
    public void onScroll_canScroll_shouldNotEnableAgreeButton() {
        ShadowDisclaimerItemFactory.setDisclaimerItemList(mDisclaimerItemList);
        when(mMockAbsListView.canScrollVertically(1)).thenReturn(true);

        mActivity.onCreate(null);
        mOnScrollListenerCaptor.getValue().onScroll(mMockAbsListView, 0, 0, 0);

        // Check the agreeButton is not enabled when not scrolled to bottom end.
        verify(mMockAgreeButton, never()).setEnabled(anyBoolean());
    }

    @Test
    public void onClick_agreeButton_shouldFinishDisclaimerActivity() {
        ShadowDisclaimerItemFactory.setDisclaimerItemList(mDisclaimerItemList);

        mActivity.onCreate(null);
        mAgreeOnClickListenerCaptor.getValue().onClick(null);

        // Check the onAgreed callback is called when agree button clicked.
        verify(mMockDisclaimerItem).onAgreed();
        // Check the WFC disclaimer activity is finished when agree button clicked.
        verify(mActivity).setResult(Activity.RESULT_OK, null);
        verify(mActivity).finish();
    }
}

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
 * limitations under the License
 */

package com.android.settings.notification;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.os.UserManager;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDynamicIndexableContentMonitor;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
                ShadowDynamicIndexableContentMonitor.class
        })
public class ZenModeScheduleRuleSettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    @Mock
    private Activity mActivity;

    @Mock
    private Intent intent;

    @Mock
    private Toast toast;

    @Mock
    private UserManager mUserManager;

    private FakeFeatureFactory mFeatureFactory;
    private TestFragment mFragment;
    private Resources.Theme theme;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);

        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        mFragment = spy(new TestFragment());
        mFragment.onAttach(ShadowApplication.getInstance().getApplicationContext());

        doReturn(mActivity).when(mFragment).getActivity();

        Resources res = application.getResources();
        theme = res.newTheme();

        doReturn(res).when(mFragment).getResources();
        when(mFragment.getActivity().getTheme()).thenReturn(theme);
        when(mFragment.getActivity().getIntent()).thenReturn(intent);
        when(mFragment.getActivity().getResources()).thenReturn(res);
        when(mFragment.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
    }

    @Test
    public void noRuleId_shouldNotCrash_toastAndFinish() {
        Context ctx = RuntimeEnvironment.application.getApplicationContext();
        String expected = ctx.getResources().getString(R.string.zen_mode_rule_not_found_text);

        mFragment.onCreate(null);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expected);
    }

    public static class TestFragment extends ZenModeScheduleRuleSettings {

        public TestFragment() {
            super();
        }

        @Override
        protected Object getSystemService(final String name) {
            return null;
        }

        @Override
        protected void maybeRefreshRules(boolean success, boolean fireChanged) {
            //do nothing
        }
    }

}

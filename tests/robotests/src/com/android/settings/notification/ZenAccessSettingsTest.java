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

import static org.junit.Assert.assertFalse;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(SettingsRobolectricTestRunner.class)
public class ZenAccessSettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private FakeFeatureFactory mFeatureFactory;
    private ZenAccessSettings mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFragment = spy(new ZenAccessSettings());
    }

    @Test
    public void logSpecialPermissionChange() {
        ZenAccessSettings.logSpecialPermissionChange(true, "app", mContext);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_DND_ALLOW),
                eq("app"));

        ZenAccessSettings.logSpecialPermissionChange(false, "app", mContext);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_DND_DENY),
                eq("app"));
    }

    @Test
    public void removeFixedNotificationPolicyAccess() {
        // Set up 3 installed apps, in which 1 is hidden module
        final List<ApplicationInfo> apps = new ArrayList<>();
        apps.add(createApplicationInfo("test.package.1"));
        apps.add(createApplicationInfo("test.hidden.module.2"));
        apps.add(createApplicationInfo("test.package.3"));

        doReturn(true).when(mFragment).isManageNotificationsPermissionGranted("test.hidden.module.2");
        doReturn(false).when(mFragment).isManageNotificationsPermissionGranted("test.package.1");
        doReturn(false).when(mFragment).isManageNotificationsPermissionGranted("test.package.3");
        mFragment.removeFixedNotificationPolicyAccess(apps);

        assertFalse(isAppInApplicationList("test.hidden.module.2", apps));
    }

    private ApplicationInfo createApplicationInfo(String packageName) {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.sourceDir = "foo";
        appInfo.flags |= ApplicationInfo.FLAG_INSTALLED;
        appInfo.storageUuid = UUID.randomUUID();
        appInfo.packageName = packageName;
        return appInfo;
    }

    private boolean isAppInApplicationList(
            String packageName, List<ApplicationInfo> applicationList) {
        for (ApplicationInfo appInfo : applicationList) {
            if (appInfo.packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}

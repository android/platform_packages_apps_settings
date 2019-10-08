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
package com.android.settings.location;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Bundle;

import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LocationSettingsTest {

    @Mock
    private SettingsActivity mActivity;
    @Mock
    private SwitchBar mSwitchBar;

    private LocationSettings mLocationSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLocationSettings = spy(new LocationSettings());
        doReturn(mActivity).when(mLocationSettings).getActivity();
        when(mActivity.getSwitchBar()).thenReturn(mSwitchBar);
    }

    @Test
    public void onActivityCreated_shouldShowSwitchBar() {
        mLocationSettings.onActivityCreated(new Bundle());

        verify(mSwitchBar).show();
    }
}

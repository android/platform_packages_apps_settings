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
 * limitations under the License.
 */

package com.android.settings.language;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.os.UserManager;
import android.provider.Settings;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.TextServicesManager;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.core.lifecycle.LifecycleObserver;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class LanguageAndInputSettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private InputManager mIm;
    @Mock
    private InputMethodManager mImm;
    @Mock
    private DevicePolicyManager mDpm;
    @Mock
    private InputMethodManager mInputMethodManager;
    @Mock
    private AutofillManager mAutofillManager;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mock(UserManager.class));
        when(mContext.getSystemService(Context.INPUT_SERVICE)).thenReturn(mock(InputManager.class));
        when(mContext.getSystemService(Context.INPUT_SERVICE)).thenReturn(mIm);
        when(mContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE))
                .thenReturn(mock(TextServicesManager.class));
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE)).thenReturn(mDpm);
        when(mContext.getSystemService(Context.INPUT_METHOD_SERVICE)).thenReturn(mImm);
        when((Object) mContext.getSystemService(AutofillManager.class))
                .thenReturn(mAutofillManager);
        mFragment = new TestFragment(mContext);
    }

    @Test
    public void testGetPreferenceScreenResId() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(R.xml.language_and_input);
    }

    @Test
    public void testGetPreferenceControllers_shouldRegisterLifecycleObservers() {
        final List<PreferenceController> controllers = mFragment.getPreferenceControllers(mContext);
        int lifecycleObserverCount = 0;
        for (PreferenceController controller : controllers) {
            if (controller instanceof LifecycleObserver) {
                lifecycleObserverCount++;
            }
        }
        verify(mFragment.getLifecycle(), times(lifecycleObserverCount))
                .addObserver(any(LifecycleObserver.class));
    }

    @Test

    public void testGetPreferenceControllers_shouldAllBeCreated() {
        final List<PreferenceController> controllers = mFragment.getPreferenceControllers(mContext);

        assertThat(controllers.isEmpty()).isFalse();
    }

    @Test
    @Config(shadows = {
            ShadowSecureSettings.class,
    })
    public void testSummary_shouldSetToCurrentImeName() {
        final Activity activity = mock(Activity.class);
        final SummaryLoader loader = mock(SummaryLoader.class);
        final ComponentName componentName = new ComponentName("pkg", "cls");
        ShadowSecureSettings.putString(null, Settings.Secure.DEFAULT_INPUT_METHOD,
                componentName.flattenToString());
        when(activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                .thenReturn(mInputMethodManager);
        when(activity.getPackageManager()).thenReturn(mPackageManager);
        final List<InputMethodInfo> imis = new ArrayList<>();
        imis.add(mock(InputMethodInfo.class));
        when(imis.get(0).getPackageName()).thenReturn(componentName.getPackageName());
        when(mInputMethodManager.getInputMethodList()).thenReturn(imis);

        SummaryLoader.SummaryProvider provider = mFragment.SUMMARY_PROVIDER_FACTORY
                .createSummaryProvider(activity, loader);

        provider.setListening(true);

        verify(imis.get(0)).loadLabel(mPackageManager);
        verify(loader).setSummary(provider, null);
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = spy(RuntimeEnvironment.application);
        //(InputManager) context.getSystemService(Context.INPUT_SERVICE);
        InputManager manager = mock(InputManager.class);
        when(manager.getInputDeviceIds()).thenReturn(new int[]{});
        doReturn(manager).when(context).getSystemService(Context.INPUT_SERVICE);
        final List<String> niks = LanguageAndInputSettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);
        final int xmlId = (new LanguageAndInputSettings()).getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context, xmlId);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testPreferenceControllers_getPreferenceKeys_existInPreferenceScreen() {
        final Context context = RuntimeEnvironment.application;
        final LanguageAndInputSettings fragment = new LanguageAndInputSettings();
        final List<String> preferenceScreenKeys = XmlTestUtils.getKeysFromPreferenceXml(context,
                fragment.getPreferenceScreenResId());
        final List<String> preferenceKeys = new ArrayList<>();

        for (PreferenceController controller : fragment.getPreferenceControllers(context)) {
            preferenceKeys.add(controller.getPreferenceKey());
        }

        assertThat(preferenceScreenKeys).containsAllIn(preferenceKeys);
    }

    /**
     * Test fragment to expose lifecycle and context so we can verify behavior for observables.
     */
    public static class TestFragment extends LanguageAndInputSettings {

        private Lifecycle mLifecycle;
        private Context mContext;

        public TestFragment(Context context) {
            mContext = context;
            mLifecycle = mock(Lifecycle.class);
            setAmbientDisplayConfig(mock(AmbientDisplayConfiguration.class));
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        protected Lifecycle getLifecycle() {
            if (mLifecycle == null) {
                return super.getLifecycle();
            }
            return mLifecycle;
        }
    }
}

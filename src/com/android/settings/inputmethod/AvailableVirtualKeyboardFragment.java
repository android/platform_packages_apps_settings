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

package com.android.settings.inputmethod;

import android.annotation.DrawableRes;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.PreferenceScreen;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.inputmethod.InputMethodAndSubtypeUtil;
import com.android.settingslib.inputmethod.InputMethodPreference;
import com.android.settingslib.inputmethod.InputMethodSettingValuesWrapper;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

public final class AvailableVirtualKeyboardFragment extends SettingsPreferenceFragment
        implements InputMethodPreference.OnSavePreferenceListener, Indexable {

    private final ArrayList<InputMethodPreference> mInputMethodPreferenceList = new ArrayList<>();
    private InputMethodSettingValuesWrapper mInputMethodSettingValues;
    private InputMethodManager mImm;
    private DevicePolicyManager mDpm;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Activity activity = getActivity();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(activity);
        screen.setTitle(activity.getString(R.string.available_virtual_keyboard_category));
        setPreferenceScreen(screen);
        mInputMethodSettingValues = InputMethodSettingValuesWrapper.getInstance(activity);
        mImm = activity.getSystemService(InputMethodManager.class);
        mDpm = activity.getSystemService(DevicePolicyManager.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh internal states in mInputMethodSettingValues to keep the latest
        // "InputMethodInfo"s and "InputMethodSubtype"s
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        updateInputMethodPreferenceViews();
    }

    @Override
    public void onSaveInputMethodPreference(final InputMethodPreference pref) {
        final boolean hasHardwareKeyboard = getResources().getConfiguration().keyboard
                == Configuration.KEYBOARD_QWERTY;
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(),
                mImm.getInputMethodList(), hasHardwareKeyboard);
        // Update input method settings and preference list.
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        for (final InputMethodPreference p : mInputMethodPreferenceList) {
            p.updatePreferenceViews();
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ENABLE_VIRTUAL_KEYBOARDS;
    }

    @Nullable
    private static Drawable loadDrawable(@NonNull final PackageManager packageManager,
            @NonNull final String packageName, @DrawableRes final int resId,
            @NonNull final ApplicationInfo applicationInfo) {
        if (resId == 0) {
            return null;
        }
        try {
            return packageManager.getDrawable(packageName, resId, applicationInfo);
        } catch (Exception e){
            return null;
        }
    }

    @NonNull
    private static Drawable getInputMethodIcon(@NonNull final PackageManager packageManager,
            @NonNull final InputMethodInfo imi) {
        final ServiceInfo si = imi.getServiceInfo();
        final ApplicationInfo ai = si != null ? si.applicationInfo : null;
        final String packageName = imi.getPackageName();
        if (si == null || ai == null || packageName == null) {
            return new ColorDrawable(Color.TRANSPARENT);
        }
        // We do not use ServiceInfo#loadLogo() and ServiceInfo#loadIcon here since those methods
        // internally have some fallback rules, which we want to do manually.
        Drawable drawable = loadDrawable(packageManager, packageName, si.logo, ai);
        if (drawable != null) {
            return drawable;
        }
        drawable = loadDrawable(packageManager, packageName, si.icon, ai);
        if (drawable != null) {
            return drawable;
        }
        // We do not use ApplicationInfo#loadLogo() and ApplicationInfo#loadIcon here since those
        // methods internally have some fallback rules, which we want to do manually.
        drawable = loadDrawable(packageManager, packageName, ai.logo, ai);
        if (drawable != null) {
            return drawable;
        }
        drawable = loadDrawable(packageManager, packageName, ai.icon, ai);
        if (drawable != null) {
            return drawable;
        }
        return new ColorDrawable(Color.TRANSPARENT);
    }

    private void updateInputMethodPreferenceViews() {
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        // Clear existing "InputMethodPreference"s
        mInputMethodPreferenceList.clear();
        List<String> permittedList = mDpm.getPermittedInputMethodsForCurrentUser();
        final Context context = getPrefContext();
        final PackageManager packageManager = getActivity().getPackageManager();
        final List<InputMethodInfo> imis = mInputMethodSettingValues.getInputMethodList();
        final int numImis = (imis == null ? 0 : imis.size());
        for (int i = 0; i < numImis; ++i) {
            final InputMethodInfo imi = imis.get(i);
            final boolean isAllowedByOrganization = permittedList == null
                    || permittedList.contains(imi.getPackageName());
            final InputMethodPreference pref = new InputMethodPreference(
                    context, imi, true, isAllowedByOrganization, this);
            pref.setIcon(getInputMethodIcon(packageManager, imi));
            mInputMethodPreferenceList.add(pref);
        }
        final Collator collator = Collator.getInstance();
        mInputMethodPreferenceList.sort((lhs, rhs) -> lhs.compareTo(rhs, collator));
        getPreferenceScreen().removeAll();
        for (int i = 0; i < numImis; ++i) {
            final InputMethodPreference pref = mInputMethodPreferenceList.get(i);
            pref.setOrder(i);
            getPreferenceScreen().addPreference(pref);
            InputMethodAndSubtypeUtil.removeUnnecessaryNonPersistentPreference(pref);
            pref.updatePreferenceViews();
        }
    }

    private static List<InputMethodSubtype> getAllSubtypesOf(final InputMethodInfo imi) {
        final int subtypeCount = imi.getSubtypeCount();
        final List<InputMethodSubtype> allSubtypes = new ArrayList<>(subtypeCount);
        for (int index = 0; index < subtypeCount; index++) {
            allSubtypes.add(imi.getSubtypeAt(index));
        }
        return allSubtypes;
    }

    static List<SearchIndexableRaw> buildSearchIndexOfInputMethods(final Context context,
            final List<InputMethodInfo> inputMethods, final String screenTitle) {
        final List<SearchIndexableRaw> indexes = new ArrayList<>();
        for (int i = 0; i < inputMethods.size(); i++) {
            final InputMethodInfo imi = inputMethods.get(i);
            final ServiceInfo serviceInfo = imi.getServiceInfo();
            final SearchIndexableRaw index = new SearchIndexableRaw(context);
            index.key = new ComponentName(serviceInfo.packageName, serviceInfo.name)
                    .flattenToString();
            index.title = imi.loadLabel(context.getPackageManager()).toString();
            index.summaryOn = index.summaryOff = InputMethodAndSubtypeUtil
                    .getSubtypeLocaleNameListAsSentence(getAllSubtypesOf(imi), context, imi);
            index.screenTitle = screenTitle;
            indexes.add(index);
        }
        return indexes;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final InputMethodManager imm = context.getSystemService(InputMethodManager.class);
            final List<InputMethodInfo> enabledInputMethods = imm.getEnabledInputMethodList();
            final List<InputMethodInfo> disabledInputMethods = new ArrayList<>();
            for (final InputMethodInfo imi : imm.getInputMethodList()) {
                if (!enabledInputMethods.contains(imi)) {
                    disabledInputMethods.add(imi);
                }
            }
            final String screenTitle = context.getString(
                    R.string.available_virtual_keyboard_category);
            return buildSearchIndexOfInputMethods(context, disabledInputMethods, screenTitle);
        }
    };
}

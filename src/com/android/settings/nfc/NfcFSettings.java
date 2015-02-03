/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.nfc;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.nfc.NfcFBackend.NfcFAppInfo;

import java.util.List;

public class NfcFSettings extends SettingsPreferenceFragment implements
        OnClickListener, OnPreferenceChangeListener {
    public static final String TAG = "NfcFSettings";
    private LayoutInflater mInflater;
    private NfcFBackend mNfcFBackend;
    private final PackageMonitor mSettingsPackageMonitor =
            new SettingsPackageMonitor();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mNfcFBackend = new NfcFBackend(getActivity());
        mInflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        setHasOptionsMenu(true);
    }

    public void refresh() {
        PreferenceManager manager = getPreferenceManager();
        PreferenceScreen screen = manager.createPreferenceScreen(getActivity());
        // Get all NfcF services
        List<NfcFAppInfo> appInfos = mNfcFBackend.getNfcFAppInfos();
        if (appInfos != null && appInfos.size() > 0) {
            // Add all NfcF apps
            for (NfcFAppInfo appInfo : appInfos) {
                NfcFAppPreference preference =
                        new NfcFAppPreference(getActivity(), appInfo, this);
                preference.setTitle(appInfo.caption);
                screen.addPreference(preference);
            }
        }

        if (screen.getPreferenceCount() == 0) {
            getListView().setVisibility(View.GONE);
        } else {
            CheckBoxPreference foreground = new CheckBoxPreference(getActivity());
            boolean foregroundMode = mNfcFBackend.isForegroundMode();
            foreground.setPersistent(false);
            foreground.setTitle(getString(R.string.nfc_nfcf_favor_foreground));
            foreground.setChecked(foregroundMode);
            foreground.setOnPreferenceChangeListener(this);
            screen.addPreference(foreground);
            getListView().setVisibility(View.VISIBLE);
        }
        setPreferenceScreen(screen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = mInflater.inflate(R.layout.nfc_nfcf, container, false);
        return v;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() instanceof NfcFAppInfo) {
            NfcFAppInfo appInfo = (NfcFAppInfo) v.getTag();
            if (appInfo.componentName != null) {
                if (appInfo.isRegistered ||
                        (!appInfo.isRegistered && appInfo.isRegisterable)) {
                    mNfcFBackend.setRegisteredNfcFApp(
                            appInfo.componentName,
                            appInfo.isRegistered ? false : true);
                    refresh();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsPackageMonitor.register(getActivity(),
                getActivity().getMainLooper(),
                false);
        refresh();
    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            refresh();
        }
    };

    private class SettingsPackageMonitor extends PackageMonitor {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            mHandler.obtainMessage().sendToTarget();
        }
    }

    public static class NfcFAppPreference extends Preference {
        private final OnClickListener listener;
        private final NfcFAppInfo appInfo;

        public NfcFAppPreference(Context context, NfcFAppInfo appInfo,
                OnClickListener listener) {
            super(context);
            setLayoutResource(R.layout.nfc_nfcf_option);
            this.appInfo = appInfo;
            this.listener = listener;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            view.setOnClickListener(listener);
            view.setTag(appInfo);

            ImageView banner = (ImageView) view.findViewById(R.id.banner);
            if (appInfo.banner == null) {
                PackageManager pm = getContext().getPackageManager();
                Drawable icon;
                try {
                    icon = pm.getApplicationIcon(
                            appInfo.componentName.getPackageName());
                } catch (NameNotFoundException e) {
                    icon = null;
                }
                banner.setImageDrawable(icon);
            } else {
                banner.setImageDrawable(appInfo.banner);
            }

            banner.setEnabled(
                    appInfo.isRegistered ? true : appInfo.isRegisterable);

            TextView textCaption = (TextView) view.findViewById(
                    R.id.nfc_nfcf_app_caption);
            textCaption.setText(appInfo.caption);
            textCaption.setEnabled(
                    appInfo.isRegistered ? true : appInfo.isRegisterable);

            TextView textInfo = (TextView) view.findViewById(
                    R.id.nfc_nfcf_app_info);
            textInfo.setText("System Code:" + appInfo.systemCode + "\n" +
                    "NFCID2:" + appInfo.nfcid2);
            textInfo.setEnabled(
                    appInfo.isRegistered ? true : appInfo.isRegisterable);

            CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
            checkBox.setChecked(appInfo.isRegistered);
            checkBox.setEnabled(
                    appInfo.isRegistered ? true : appInfo.isRegisterable);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof CheckBoxPreference) {
            mNfcFBackend.setForegroundMode(((Boolean) newValue).booleanValue());
            return true;
        } else {
            return false;
        }
    }
}

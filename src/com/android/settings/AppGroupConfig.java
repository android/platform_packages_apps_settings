/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationGroup;
import android.app.ProfileManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class AppGroupConfig extends PreferenceActivity {

    private static final int DIALOG_APPS = 0;
    private static final int DELETE_CONFIRM = 1;

    private ListView mListView;

    private PackageManager mPackageManager;

    private PackageAdaptor mPackageAdaptor;

    private List<PackageInfo> mInstalledPackages;

    private NotificationGroup mNotificationGroup;

    private ProfileManager mProfileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProfileManager = (ProfileManager) this.getSystemService(PROFILE_SERVICE);

        mNotificationGroup = (NotificationGroup) getIntent().getExtras().getParcelable(
                "NotificationGroup");

        setTitle(getString(R.string.profile_appgroup_title, mNotificationGroup.getName()));

        mPackageManager = getPackageManager();

        addPreferencesFromResource(R.xml.application_list);

        mInstalledPackages = mPackageManager.getInstalledPackages(0);

        updatePackages();

    }

    Preference mAddPreference;

    Preference mDeletePreference;

    private void updatePackages() {

        mAddPreference = (PreferenceScreen) findPreference("profile_add_app");
        mDeletePreference = (PreferenceScreen) findPreference("profile_delete_appgroup");

        PreferenceGroup profileList = (PreferenceGroup) findPreference("profile_applist_title");
        profileList.removeAll();

        for (String pkg : mNotificationGroup.getPackages()) {

            ApplicationItemPreference pref = new ApplicationItemPreference(this);

            try {
                PackageInfo group = mPackageManager.getPackageInfo(pkg, 0);
                pref.setKey(group.packageName);
                pref.setTitle(group.applicationInfo.loadLabel(mPackageManager));
                Drawable icon = group.applicationInfo.loadIcon(mPackageManager);
                pref.setIcon(icon);
                pref.setSelectable(true);
                pref.setPersistent(false);

                profileList.addPreference(pref);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, R.string.profile_menu_delete, 0, R.string.profile_menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo aMenuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        PackageItem selectedGroup = (PackageItem) mListView.getItemAtPosition(aMenuInfo.position);
        switch (item.getItemId()) {
            case R.string.profile_menu_delete:
                deleteAppFromGroup(selectedGroup);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAppFromGroup(PackageItem selectedGroup) {
        if (selectedGroup != null) {
            mNotificationGroup.removePackage(selectedGroup.packageName);
            updatePackages();
        }
    }

    @Override
    protected void onPause() {
        if (mNotificationGroup != null) {
            mProfileManager.addNotificationGroup(mNotificationGroup);
        }
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PreferenceScreen) {
            if (mAddPreference == preference) {
                addNewApp();
                return true;
            } else if (mDeletePreference == preference) {
                mProfileManager.removeNotificationGroup(mNotificationGroup);
                mNotificationGroup = null;
                finish();
                return true;
            }
        }else if (preference instanceof ApplicationItemPreference){
            String deleteItem = preference.getKey();
            Bundle bundle = new Bundle();
            bundle.putString("key", deleteItem);
            showDialog(DELETE_CONFIRM, bundle);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void addNewApp() {
        showDialog(DIALOG_APPS);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Dialog dialog;
        switch (id) {
            case DIALOG_APPS:
                final ListView list = new ListView(this);
                PackageAdaptor adapter = new PackageAdaptor(mInstalledPackages);
                list.setAdapter(adapter);
                adapter.update();
                builder.setMessage(R.string.profile_choose_app);
                builder.setView(list);
                dialog = builder.create();
                list.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        mNotificationGroup.addPackage(info.packageName);
                        updatePackages();
                        dialog.cancel();
                    }
                });
                break;
            case DELETE_CONFIRM:
                builder.setMessage(R.string.profile_app_delete_confirm);
                builder.setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doDelete();
                            }
                        });
                builder.setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                dialog = builder.create();
                break;
            default:
                dialog = null;
        }
        return dialog;
    }
    
    String mPackageToDelete;
    

    private void doDelete() {
        mNotificationGroup.removePackage(mPackageToDelete);
        updatePackages();
    }
    
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch(id){
            case DELETE_CONFIRM :
                mPackageToDelete = args.getString("key");
        }
        super.onPrepareDialog(id, dialog, args);
    }

    class PackageItem implements Comparable<PackageItem> {
        CharSequence title;

        String packageName;

        Drawable icon;

        @Override
        public int compareTo(PackageItem another) {
            return this.title.toString().compareTo(another.title.toString());
        }
    }

    class PackageAdaptor extends BaseAdapter {

        protected List<PackageInfo> mInstalledPackageInfo;

        protected List<PackageItem> mInstalledPackages = new LinkedList<PackageItem>();

        private void reloadList() {
            final Handler handler = new Handler();
            new Thread(new Runnable() {

                @Override
                public void run() {
                    synchronized (mInstalledPackages) {
                        mInstalledPackages.clear();
                        for (PackageInfo info : mInstalledPackageInfo) {
                            final PackageItem item = new PackageItem();
                            ApplicationInfo applicationInfo = info.applicationInfo;
                            item.title = applicationInfo.loadLabel(mPackageManager);
                            item.icon = applicationInfo.loadIcon(mPackageManager);
                            item.packageName = applicationInfo.packageName;
                            handler.post(new Runnable() {

                                @Override
                                public void run() {
                                    int index = Collections.binarySearch(mInstalledPackages, item);
                                    if (index < 0) {
                                        index = -index - 1;
                                        mInstalledPackages.add(index, item);
                                    }
                                    notifyDataSetChanged();
                                }
                            });
                        }
                    }
                }
            }).start();
        }

        public PackageAdaptor(List<PackageInfo> installedPackagesInfo) {
            mInstalledPackageInfo = installedPackagesInfo;
        }

        public void update() {
            reloadList();
        }

        @Override
        public int getCount() {
            return mInstalledPackages.size();
        }

        @Override
        public PackageItem getItem(int position) {
            return mInstalledPackages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mInstalledPackages.get(position).packageName.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.preference_icon, null, false);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.title = (TextView) convertView.findViewById(com.android.internal.R.id.title);
                holder.summary = (TextView) convertView
                        .findViewById(com.android.internal.R.id.summary);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            }
            PackageItem applicationInfo = getItem(position);

            if (holder.title != null) {
                holder.title.setText(applicationInfo.title);
            }
            if (holder.summary != null) {
                holder.summary.setVisibility(View.GONE);
            }
            if (holder.icon != null) {
                Drawable loadIcon = applicationInfo.icon;
                holder.icon.setImageDrawable(loadIcon);
                holder.icon.setAdjustViewBounds(true);
                holder.icon.setMaxHeight(72);
                holder.icon.setMaxWidth(72);
            }
            return convertView;
        }

    }

    static class ViewHolder {
        TextView title;

        TextView summary;

        ImageView icon;
    }

}

/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Permissions extends ExpandableListActivity implements Runnable {
    private static final String TAG = "Permissions";

    private static final String NAME = "Name";
    private static final String DESCRIPTION = "Description";
    private static final String PACKAGENAME = "PackageName";
    private static final String SECURITYLEVEL = "Securitylevel";

    private int mDangerousColor;
    private int mDefaultTextColor;

    private static final int PROGRESS_DIALOG = 0;
    private ProgressDialog progressDialog;

    private List<Map<String, String>> groupData;
    private List<List<Map<String, String>>> childData;

    private PackageManager pm;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        mDangerousColor = getResources().getColor(
                com.android.internal.R.color.perms_dangerous_grp_color);
        groupData = new ArrayList<Map<String, String>>();
        childData = new ArrayList<List<Map<String, String>>>();
        // TODO: is there a better way to get the default TextColor?
        TextView temp = new TextView(this);
        mDefaultTextColor = temp.getCurrentTextColor();
    };

    @Override
    protected void onResume() {
        super.onResume();
        groupData.clear();
        childData.clear();
        showDialog(PROGRESS_DIALOG);
    }

    @Override
    protected void onPause() {
        removeDialog(PROGRESS_DIALOG);
        super.onPause();
    };

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
            int childPosition, long id) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, InstalledAppDetails.class);
        intent.putExtra(ManageApplications.APP_PKG_NAME, (String)v.getTag());
        startActivity(intent);
        return super.onChildClick(parent, v, groupPosition, childPosition, id);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case PROGRESS_DIALOG:
                progressDialog = new ProgressDialog(this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage(getText(R.string.permission_settings_loading));
                Thread thread = new Thread(this);
                thread.start();
                return progressDialog;
            default:
                return super.onCreateDialog(id, null);
        }
    }

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            removeDialog(PROGRESS_DIALOG);
            PermissionAdapter mAdapter = new PermissionAdapter(
                    Permissions.this,
                    groupData,
                    R.layout.permissions_expandable_list_item,
                    new String[] { NAME, DESCRIPTION },
                    new int[] { android.R.id.text1, android.R.id.text2 },
                    childData,
                    R.layout.permissions_expandable_list_item_child,
                    new String[] { NAME, DESCRIPTION },
                    new int[] { android.R.id.text1, android.R.id.text2 }
                    );
            setListAdapter(mAdapter);
        }
    };

    private class PermissionAdapter extends SimpleExpandableListAdapter {
        public PermissionAdapter(Context context, List<? extends Map<String, ?>> groupData,
                int groupLayout, String[] groupFrom, int[] groupTo,
                List<? extends List<? extends Map<String, ?>>> childData, int childLayout,
                String[] childFrom, int[] childTo) {
            super(context, groupData, groupLayout, groupFrom, groupTo, childData,
                    childLayout, childFrom, childTo);
        }

        @Override
        @SuppressWarnings("unchecked")
        public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            final View v = super.getGroupView(groupPosition, isExpanded, convertView, parent);
            Map<String, String> group = (Map<String, String>) getGroup(groupPosition);
            int secLevel = Integer.parseInt(group.get(SECURITYLEVEL));
            TextView textView = (TextView) v.findViewById(android.R.id.text1);
            if (PermissionInfo.PROTECTION_DANGEROUS == secLevel) {
                textView.setTextColor(mDangerousColor);
            } else {
                textView.setTextColor(mDefaultTextColor);
            }
            return v;
        }

        @Override
        @SuppressWarnings("unchecked")
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            final View v = super.getChildView(groupPosition, childPosition, isLastChild,
                    convertView, parent);

            ImageView imageView = (ImageView) v.findViewById(android.R.id.icon);
            Map<String, String> child =
                (Map<String, String>)getChild(groupPosition, childPosition);
            Drawable icon;
            String packageName = (String)child.get(PACKAGENAME);
            try {
                icon = pm.getApplicationIcon(packageName);
            } catch (NameNotFoundException e) {
                icon = pm.getDefaultActivityIcon();
            }
            imageView.setImageDrawable(icon);
            v.setTag(packageName);
            return v;
        }
    }

    public void run() {
        List<PackageInfo> appList =
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        Map<String, List<PackageInfo>> permList = new TreeMap<String, List<PackageInfo>>();
        for (PackageInfo pi : appList) {
            // Do not add System Packages
            if (pi.requestedPermissions == null || pi.packageName.equals("android")) {
                continue;
            }
            for (String perms : pi.requestedPermissions) {
                if (!permList.containsKey(perms)) {
                    // First time we get this permission so add it and create a new List
                    permList.put(perms, new ArrayList<PackageInfo>());
                }
                permList.get(perms).add(pi);
            }
        }

        Set<String> keys = permList.keySet();
        for (String key : keys) {
            Map<String, String> curGroupMap = new HashMap<String, String>();
            try {
                PermissionInfo pinfo =
                    pm.getPermissionInfo(key, PackageManager.GET_META_DATA);
                CharSequence label = pinfo.loadLabel(pm);
                CharSequence desc = pinfo.loadDescription(pm);
                curGroupMap.put(NAME, (label == null) ? "" : label.toString());
                curGroupMap.put(DESCRIPTION, (desc == null) ? "" : desc.toString());
                curGroupMap.put(SECURITYLEVEL, String.valueOf(pinfo.protectionLevel));
            } catch (NameNotFoundException e) {
                Log.i(TAG, "Ignoring unknown permission " + key);
                continue;
            }
            groupData.add(curGroupMap);
            List<Map<String, String>> children = new ArrayList<Map<String, String>>();
            List<PackageInfo> infos = permList.get(key);
            for (PackageInfo child : infos) {
                Map<String, String> curChildMap = new HashMap<String, String>();
                String appName = (child.applicationInfo == null) ?
                        child.packageName : child.applicationInfo.loadLabel(pm).toString();
                curChildMap.put(NAME, appName);
                curChildMap.put(DESCRIPTION, child.versionName);
                curChildMap.put(PACKAGENAME, child.packageName);
                children.add(curChildMap);
            }
            childData.add(children);
        }
        handler.sendEmptyMessage(0);
    }
}

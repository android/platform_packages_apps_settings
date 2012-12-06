/**
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2009-2010 Broadcom Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/*
 * Start - Added by BrcmVT (2012/08/25)
 */

package com.android.settings;

import com.android.settings.R;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.os.PatternMatcher;
import android.net.Uri;
import android.util.Log;

import java.util.Set;
import java.util.List;
import java.util.Iterator;



public class PreferredActivityReceiver extends BroadcastReceiver {
    public static final String TAG = "PreferredActivityReceiver";
    public static final String ACTION_PREFERRED_DIAL = "com.android.settings.ACTION_PREFERRED_DIAL";
    public static final String PKG_NAME = "package_name";
    public static final String CLS_NAME = "class_name";
    public static final String OPERATION = "operation";
    //Preferred dial selection items
    private static final int PREFERRED_DIAL_NONE = 0;
    private static final int PREFERRED_DIAL_VOICE = 1;
    private static final int PREFERRED_DIAL_VIDEO = 2;

    private PackageManager mPm = null;

    @Override
    public void onReceive(Context context, Intent intent) {


        if (null == mPm) {
            mPm = context.getPackageManager();
        }

        if (intent.getAction().equals(ACTION_PREFERRED_DIAL)) {
            String packageName = intent.getStringExtra(PKG_NAME);
            String className = intent.getStringExtra(CLS_NAME);

            switch(intent.getIntExtra(OPERATION, PREFERRED_DIAL_NONE)) {
              case PREFERRED_DIAL_NONE:
                {
                    if (null != packageName) {
                        clearPackagePreferredActivitis(packageName);
                    }
                }
                break;
              case PREFERRED_DIAL_VIDEO:
              case PREFERRED_DIAL_VOICE:
                {
                    if (null != packageName && null != className) {
                        clearPackagePreferredActivitis(packageName);
                        addPreferredDialActivity(packageName, className, context);
                    }
                }
                break;
              default:
                break;
            }
        }
    }

    private void clearPackagePreferredActivitis(String pkg) {
        mPm.clearPackagePreferredActivities(pkg);
    }

    private void addPreferredDialActivity(String pkg, String cls, Context context) {
        // Initialize an intent to be added
              Intent intent = new Intent("com.android.phone.action.ACTION_VT_VOICE_CALL");
        intent.setData(Uri.fromParts("tel", "10010", null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Get resolve info from package manager
        List<ResolveInfo> rList = mPm.queryIntentActivities(
            intent, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
        ResolveInfo ri = rList.get(0);

        intent.setComponent(new ComponentName(pkg, cls));

        // Build a reasonable intent filter, based on what matched.
        IntentFilter filter = new IntentFilter();

        filter.addAction(intent.getAction());
        Set<String> categories = intent.getCategories();
        if (categories != null) {
            for (String cat : categories) {
                filter.addCategory(cat);
            }
        }
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        int cat = ri.match & IntentFilter.MATCH_CATEGORY_MASK;
        Uri data = intent.getData();
        if (cat == IntentFilter.MATCH_CATEGORY_TYPE) {
            String mimeType = intent.resolveType(context);
            if (mimeType != null) {
                try {
                    filter.addDataType(mimeType);
                } catch (IntentFilter.MalformedMimeTypeException e) {
                    Log.w(TAG, e);
                    filter = null;
                }
            }
        }

        if (data != null && data.getScheme() != null) {
            if (cat != IntentFilter.MATCH_CATEGORY_TYPE
                || (!"file".equals(data.getScheme())
                    && !"content".equals(data.getScheme()))) {
                filter.addDataScheme(data.getScheme());

                // Look through the resolved filter to determine which part
                // of it matched the original Intent.
                Iterator<IntentFilter.AuthorityEntry> aIt = ri.filter.authoritiesIterator();
                if (aIt != null) {
                    while (aIt.hasNext()) {
                        IntentFilter.AuthorityEntry a = aIt.next();
                        if (a.match(data) >= 0) {
                            int port = a.getPort();
                            filter.addDataAuthority(a.getHost(),
                                                    port >= 0 ? Integer.toString(port) : null);
                            break;
                        }
                    }
                }
                Iterator<PatternMatcher> pIt = ri.filter.pathsIterator();
                if (pIt != null) {
                    String path = data.getPath();
                    while (path != null && pIt.hasNext()) {
                        PatternMatcher p = pIt.next();
                        if (p.match(path)) {
                            filter.addDataPath(p.getPath(), p.getType());
                            break;
                        }
                    }
                }
            }
        }

        // Find best match for the component and add to preferred activity
        if (filter != null) {
            final int N = rList.size();
            ComponentName[] set = new ComponentName[N];
            int bestMatch = 0;
            for (int i = 0; i < N; i++) {
                ResolveInfo r = rList.get(i);
                set[i] = new ComponentName(r.activityInfo.packageName,
                                           r.activityInfo.name);
                if (r.match > bestMatch) bestMatch = r.match;
            }
            mPm.addPreferredActivity(filter, bestMatch, set,
                                                     intent.getComponent());
        }
    }
}

/*
 * End - Added by BrcmVT (2012/08/25)
 */

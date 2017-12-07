/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.network;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.database.Cursor;
import android.app.Activity;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;

import static com.android.settings.network.ApnSettings.mimsi_edit_action;


import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class ApnPreference extends Preference implements
        CompoundButton.OnCheckedChangeListener, OnClickListener {
    final static String TAG = "ApnPreference";
    static Context mContext;


    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    public ApnPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
	mContext = context;
    }

    private static Activity scanForActivity(Context cont) {
	if (cont == null)
	    return null;
	else if (cont instanceof Activity)
	    return (Activity) cont;
	else if (cont instanceof ContextWrapper)
	    return scanForActivity(((ContextWrapper) cont).getBaseContext());

	return null;
    }


    public ApnPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.apnPreferenceStyle);
    }

    public ApnPreference(Context context) {
        this(context, null);
    }

    private static String mSelectedKey = null;
    private static CompoundButton mCurrentChecked = null;
    private boolean mProtectFromCheckedChange = false;
    private boolean mSelectable = true;

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        View widget = view.findViewById(R.id.apn_radiobutton);
        if ((widget != null) && widget instanceof RadioButton) {
            RadioButton rb = (RadioButton) widget;
            if (mSelectable) {
                rb.setOnCheckedChangeListener(this);

                boolean isChecked = getKey().equals(mSelectedKey);
                if (isChecked) {
                    mCurrentChecked = rb;
                    mSelectedKey = getKey();
                }

                mProtectFromCheckedChange = true;
                rb.setChecked(isChecked);
                mProtectFromCheckedChange = false;
                rb.setVisibility(View.VISIBLE);
            } else {
                rb.setVisibility(View.GONE);
            }
        }

        View textLayout = view.findViewById(R.id.text_layout);
        if ((textLayout != null) && textLayout instanceof RelativeLayout) {
            textLayout.setOnClickListener(this);
        }
    }

    public boolean isChecked() {
        return getKey().equals(mSelectedKey);
    }

    public void setChecked() {
        mSelectedKey = getKey();
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i(TAG, "ID: " + getKey() + " :" + isChecked);
        if (mProtectFromCheckedChange) {
            return;
        }

        if (isChecked) {
            if (mCurrentChecked != null) {
                mCurrentChecked.setChecked(false);
            }
            mCurrentChecked = buttonView;
            mSelectedKey = getKey();
            callChangeListener(mSelectedKey);
        } else {
            mCurrentChecked = null;
            mSelectedKey = null;
        }
    }

    public void onClick(android.view.View v) {
        if ((v != null) && (R.id.text_layout == v.getId())) {
            Context context = getContext();
            if (context != null) {
		// check if we are in mimsi mode
		Intent editIntent;
		if (ApnSettings.isMimsiSelected()) {
		    int pos = Integer.parseInt(getKey());
		    Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);

		    // retrieve Apn information in URI
		    Activity activity = scanForActivity(mContext);
		    Cursor cursor = activity.managedQuery(url, new String[]{
			    "apn"}, null, null);
		    cursor.moveToFirst();
		    String apn = cursor.getString(0);

		    editIntent = new Intent(mimsi_edit_action);
		    editIntent.putExtra("APN", apn);

		} else {
		    int pos = Integer.parseInt(getKey());
		    Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
		    editIntent = new Intent(Intent.ACTION_EDIT, url);
		    editIntent.putExtra(ApnSettings.SUB_ID, mSubId);
		}
                context.startActivity(editIntent);
            }
        }
    }

    public void setSelectable(boolean selectable) {
        mSelectable = selectable;
    }

    public boolean getSelectable() {
        return mSelectable;
    }

    public void setSubId(int subId) {
        mSubId = subId;
    }
}

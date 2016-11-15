/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.NetworkTemplate;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.CustomDialogPreferenceCompat;

public class CellDataPreference extends CustomDialogPreferenceCompat implements TemplatePreference {

    private static final String TAG = "CellDataPreference";

    public int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    public boolean mChecked;
    private TelephonyManager mTelephonyManager;
    @VisibleForTesting
    SubscriptionManager mSubscriptionManager;

    public CellDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(context,
                androidx.preference.R.attr.switchPreferenceStyle,
                android.R.attr.switchPreferenceStyle));
    }

    @Override
    protected void onRestoreInstanceState(Parcelable s) {
        CellDataState state = (CellDataState) s;
        super.onRestoreInstanceState(state.getSuperState());
        mTelephonyManager = TelephonyManager.from(getContext());
        mChecked = state.mChecked;
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mSubId = state.mSubId;
            setKey(getKey() + mSubId);
        }
        notifyChanged();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        CellDataState state = new CellDataState(super.onSaveInstanceState());
        state.mChecked = mChecked;
        state.mSubId = mSubId;
        return state;
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mDataStateListener.setListener(true, mSubId, getContext());
        if (mSubscriptionManager!= null) {
            mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        }
    }

    @Override
    public void onDetached() {
        mDataStateListener.setListener(false, mSubId, getContext());
        if (mSubscriptionManager!= null) {
            mSubscriptionManager.removeOnSubscriptionsChangedListener(
                    mOnSubscriptionsChangeListener);
        }
        super.onDetached();
    }

    @Override
    public void setTemplate(NetworkTemplate template, int subId, NetworkServices services) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            throw new IllegalArgumentException("CellDataPreference needs a SubscriptionInfo");
        }
        mSubscriptionManager = SubscriptionManager.from(getContext());
        mTelephonyManager = TelephonyManager.from(getContext());

        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mSubId = subId;
            setKey(getKey() + subId);
        }
        updateEnabled();
        updateChecked();
    }

    private void updateChecked() {
        setChecked(mTelephonyManager.getDataEnabled(mSubId));
    }

    private void updateEnabled() {
        // Disable the button if the user is not the system user of the device or if the
        // subscription is not active, for example, SIM card is taken out.
        setEnabled(mSubscriptionManager.getActiveSubscriptionInfo(mSubId) != null
                && ActivityManager.getCurrentUser() == UserHandle.USER_SYSTEM);
    }

    @Override
    protected void performClick(View view) {
        final Context context = getContext();
        FeatureFactory.getFactory(context).getMetricsFeatureProvider()
                .action(context, SettingsEnums.ACTION_CELL_DATA_TOGGLE, !mChecked);

        Intent intent = new Intent(TelephonyIntents.ACTION_MOBILE_DATA_TOGGLE);
        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, mSubId);
        context.sendBroadcast(intent);
        return;
    }

    private void setChecked(boolean checked) {
        if (mChecked == checked) return;
        mChecked = checked;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View switchView = holder.findViewById(android.R.id.switch_widget);
        switchView.setClickable(false);
        ((Checkable) switchView).setChecked(mChecked);
    }

    @VisibleForTesting
    final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DataUsageSummary.LOGD) {
                Log.d(TAG, "onSubscriptionsChanged");
            }
            updateEnabled();
        }
    };

    private final DataStateListener mDataStateListener = new DataStateListener() {
        @Override
        public void onChange(boolean selfChange) {
            updateChecked();
        }
    };

    public abstract static class DataStateListener extends ContentObserver {
        public DataStateListener() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void setListener(boolean listening, int subId, Context context) {
            if (listening) {
                Uri uri = Global.getUriFor(Global.MOBILE_DATA);
                if (TelephonyManager.getDefault().getSimCount() != 1) {
                    uri = Global.getUriFor(Global.MOBILE_DATA + subId);
                }
                context.getContentResolver().registerContentObserver(uri, false, this);
            } else {
                context.getContentResolver().unregisterContentObserver(this);
            }
        }
    }

    public static class CellDataState extends BaseSavedState {
        public int mSubId;
        public boolean mChecked;

        public CellDataState(Parcelable base) {
            super(base);
        }

        public CellDataState(Parcel source) {
            super(source);
            mChecked = source.readByte() != 0;
            mSubId = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (mChecked ? 1 : 0));
            dest.writeInt(mSubId);
        }

        public static final Creator<CellDataState> CREATOR = new Creator<CellDataState>() {
            @Override
            public CellDataState createFromParcel(Parcel source) {
                return new CellDataState(source);
            }

            @Override
            public CellDataState[] newArray(int size) {
                return new CellDataState[size];
            }
        };
    }
}

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
 * limitations under the License.
 */

package com.android.settings;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.settings.CustomListPreference;
import com.android.settings.R;

/**
 * ListPreference contain the entry summary.
 */
public class ListWithEntrySummaryPreference extends CustomListPreference {
    private static final String LOG_TAG = "ListWithEntrySummaryPreference";
    private final Context mContext;
    private CharSequence[] mSummaries;

    /**
     * ListWithEntrySummaryPreference constructor.
     *
     * @param context The context of view.
     * @param attrs The attributes of the XML tag that is inflating the linear layout.
     */
    public ListWithEntrySummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        TypedArray array = context.obtainStyledAttributes(attrs,
                R.styleable.ListWithEntrySummaryPreference, 0, 0);
        mSummaries = array.getTextArray(R.styleable.ListWithEntrySummaryPreference_entrySummaries);
        array.recycle();
    }

    /**
     * Sets the summaries of mode items to be shown in the mode select dialog.
     *
     * @param summariesResId The summaries of mode items.
     */
    public void setEntrySummaries(int summariesResId) {
        mSummaries = getContext().getResources().getTextArray(summariesResId);
    }

    private CharSequence getEntrySummary(int index) {
        if (mSummaries == null) {
            Log.w(LOG_TAG, "getEntrySummary : mSummaries is null");
            return "";
        }
        return mSummaries[index];
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener listener) {
        ListAdapter la = (ListAdapter) new SelectorAdapter(mContext,
                R.xml.single_choice_list_item_2, this);
        builder.setSingleChoiceItems(la, findIndexOfValue(getValue()), listener);
        super.onPrepareDialogBuilder(builder, listener);
    }

    private static class SelectorAdapter extends ArrayAdapter<CharSequence> {
        private final Context mContext;
        private ListWithEntrySummaryPreference mSelector;

        /**
         * SelectorAdapter constructor.
         *
         * @param context The current context.
         * @param rowResourceId The resource id of the XML tag that is inflating the linear layout.
         * @param listPreference The instance of ListWithEntrySummaryPreference.
         */
        public SelectorAdapter(Context context, int rowResourceId,
                ListWithEntrySummaryPreference listPreference) {
            super(context, rowResourceId, listPreference.getEntryValues());
            mContext = context;
            mSelector = listPreference;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View row = inflater.inflate(R.xml.single_choice_list_item_2, parent, false);

            TextView title = (TextView) row.findViewById(R.id.title);
            title.setText(mSelector.getEntries()[position]);

            TextView summary = (TextView) row.findViewById(R.id.summary);
            summary.setText(mSelector.getEntrySummary(position));

            RadioButton rb = (RadioButton) row.findViewById(R.id.radio);
            if (position == mSelector.findIndexOfValue(mSelector.getValue())) {
                rb.setChecked(true);
            }

            return row;
        }
    }
}

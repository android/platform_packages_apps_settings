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

package com.android.settings.wfc;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.WifiCallingSettings;
import com.android.settings.R;

/**
 * Activity for displaying disclaimers for WFC.
 */
public class DisclaimerActivity extends Activity {
    private List<DisclaimerItem> mDisclaimerItemList = new ArrayList<DisclaimerItem>();
    private Button mAgreeButton;
    private boolean mScrollToBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int phoneId = getIntent().getExtras().getInt(WifiCallingSettings.EXTRA_PHONE_ID,
                SubscriptionManager.DEFAULT_PHONE_INDEX);
        mDisclaimerItemList = DisclaimerItemFactory.create(this, phoneId);
        if (mDisclaimerItemList.size() <= 0) {
            setResult(RESULT_OK, null);
            finish();
            return;
        }

        setContentView(R.layout.wfc_disclaimer_activity);

        DisclaimerItemListAdapter adapter = new DisclaimerItemListAdapter(this,
                mDisclaimerItemList);
        final ListView disclaimerItemListView = (ListView) findViewById(R.id.disclamer_item_list);
        disclaimerItemListView.setAdapter(adapter);
        disclaimerItemListView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                if (!view.canScrollVertically(1 /* DOWN */)) {
                    mScrollToBottom = true;
                    updateButtonState();
                    disclaimerItemListView.setOnScrollListener(null);
                }
            }
        });

        mAgreeButton = (Button) findViewById(R.id.agree_button);
        mAgreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (DisclaimerItem item : mDisclaimerItemList) {
                    item.onAgreed();
                }
                setResult(RESULT_OK, null);
                finish();
            }
        });

        findViewById(R.id.disagree_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateButtonState();
    }

    private void updateButtonState() {
        mAgreeButton.setEnabled(mScrollToBottom);
    }

    @VisibleForTesting
    static class DisclaimerItemListAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private final List<DisclaimerItem> mList;

        DisclaimerItemListAdapter(Context context, List<DisclaimerItem> list) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mList.get(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.wfc_disclaimer_item_container, parent,
                        false);
            }
            LinearLayout container = (LinearLayout) convertView.findViewById(
                    R.id.disclaimer_item_container);
            container.removeAllViews();
            container.addView(mList.get(position).getView());
            return convertView;
        }
    }
}

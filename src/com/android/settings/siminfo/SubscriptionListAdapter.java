package com.android.settings.siminfo;

import android.content.Context;
import android.telephony.SubscriptionController.SubInfoRecord;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.android.internal.widget.SubscriptionView;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionListAdapter extends BaseAdapter {

    private List<SubInfoRecord> mList = new ArrayList<SubInfoRecord>();
    private Context mContext;
    
    public SubscriptionListAdapter(Context context) {
        mContext = context;
    }
    
    public SubscriptionListAdapter(Context context, final List<SubInfoRecord> simInfoList) {
        mContext = context;
        mList = simInfoList;
    }
    
    public void setData(List<SubInfoRecord> dataList) {
        mList = dataList;
        notifyDataSetChanged();
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
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SubscriptionView subView;
        if (convertView == null) {
            subView = new SubscriptionView(mContext);
        } else {
            subView = (SubscriptionView) convertView;
        }
        SubInfoRecord subInfoRecord = mList.get(position);
        subView.setSubInfo(subInfoRecord);
        return subView;
    }

}

package com.android.settings.siminfo;

import android.app.Activity;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.telephony.SubInfoRecord;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import java.util.ArrayList;
import java.util.List;


public class SubSelectSettings extends ListFragment {
    
    private static final String TAG = "SubSelectSettings";
    private SubscriptionListAdapter mAdapter;
    private List<Integer> mSimIdList = new ArrayList<Integer>();
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                String state = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int simId = intent.getIntExtra(PhoneConstants.SLOT_KEY, -1);
                handleSimStateChange(simId,state);
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Customize title if intent with extra
        String title = getActivity().getIntent().getStringExtra(Intent.EXTRA_TITLE);
        if (!TextUtils.isEmpty(title)) {
            getActivity().setTitle(title);
        }
        mAdapter = new SubscriptionListAdapter(getActivity());
        recordSimIds();
    }

    private void handleSimStateChange(int simId, String state) {
        Log.d(TAG,"handleSimStateChange with sim " + simId + " in state " + state);
        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(state)) {
            // Handle SIM cards Plug out and if simId is one of record SIM IDs, finish fragment 
            for (Integer value : mSimIdList) {
                if (value == simId) {
                    getActivity().onBackPressed();
                }
            }
        }
    }
    
    /**
     * Record current inserted SIM Ids
     */
    private void recordSimIds() {
        List<SubInfoRecord> subInfoList = SubscriptionManager.getActivatedSubInfoList(getActivity());
        if (subInfoList != null) {
            for (SubInfoRecord subInfo : subInfoList) {
                mSimIdList.add(subInfo.mSimId);
            }    
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        List<SubInfoRecord> subInfoList = SubscriptionSettingUtility.getSortedSubInfoList(getActivity());
        //Handle SIM Hot plug in/out case, when creating view possible there is no SIM available
        if (subInfoList != null && subInfoList.size() > 0) {
            mAdapter.setData(subInfoList);
            setListAdapter(mAdapter);
        } else {
            getActivity().finish();
        }
        getActivity().registerReceiver(mReceiver, new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        Intent intent = new Intent();
        SubInfoRecord subInfo = (SubInfoRecord) mAdapter.getItem(position);
        int simId = subInfo.mSimId;
        long subId = subInfo.mSubId;
        Log.d(TAG,"onListItemClick with simId = " + simId + " subId = " + subId);
        intent.putExtra(PhoneConstants.SLOT_KEY, simId);
        intent.putExtra(PhoneConstants.SUBSRIPTION_KEY, subId);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }
}
/*
 *TODO: This is temp solution for selecting SIM card, in next version it will include 
 *more UI elements of SIM information 
 */
package com.android.settings.siminfo;

import android.R;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SimInfoManager;
import android.telephony.SimInfoManager.SimInfoRecord;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class SelectSimCardSettings extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<SimInfoRecord> simInfoList = SimInfoManager.getInsertedSimInfoList(this);
        List<String> name = new ArrayList<String>();
        int i = 0;
        for (SimInfoRecord simInfo : simInfoList) {
            name.add(simInfo.mDisplayName);
        }
        getListView().setAdapter(new ArrayAdapter(this,R.layout.simple_list_item_1,name));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent();
        intent.putExtra(SimInfoUtils.SIM_ID, position);
        intent.putExtra(SimInfoUtils.SIM_INDEX, SimInfoUtils.getSimIndexBySimId(this,position));
        setResult(RESULT_OK, intent);
        finish();
    }
    
}

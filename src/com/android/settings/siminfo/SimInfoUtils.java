package com.android.settings.siminfo;

import android.content.Context;
import android.content.Intent;
import android.telephony.SimInfoManager;
import android.telephony.SimInfoManager.SimInfoRecord;

import java.util.List;

public class SimInfoUtils {
    
    public static final String SIM_INDEX = "simIndex";
    public static final String SIM_ID = "simId";
    public static final String SIM_ID_STATE = "sim_id_state";
    
    private static final String SELECT_SIM_CARD_ACT = "com.android.settings.SELECT_SIM";
    
    public static Intent getSelectSimIntent() {
        return new Intent(SELECT_SIM_CARD_ACT);
    }

    /**
     * Use SIM Id to get corresponding SIM index 
     * @param context The calling context being used to instantiate the SimInfoRecord 
     * @param simId The SIM id 
     * @return Return the related SIM index in DB
     */
    public static long getSimIndexBySimId(Context context, int simId) {
        final SimInfoRecord siminfoRecord = SimInfoManager.getSimInfoBySimId(context, simId);
        long simIndex = 0;
        if (siminfoRecord != null) {
            simIndex = siminfoRecord.mSimInfoIdx;
        }
        return simIndex;
    }

    /**
     * Get SIM id if only one sim card inserted, directly return the inserted SIM card id, otherwise, return
     * the SIM_ID extra from intent 
     * @param context The calling context use to get SimInfoRecord list
     * @param intent The intent includes the extra SIM_ID
     * @return The SIM id if specified or SIM_NOT_INSERTED (-1) 
     */
    public static int getSimId(Context context, Intent intent) {
        final List<SimInfoRecord> simInfoList = SimInfoManager.getInsertedSimInfoList(context);
        if (simInfoList.size() == 1) {
            return simInfoList.get(0).mSimId;
        }
        return intent.getIntExtra(SimInfoManager.SIM_ID, SimInfoManager.SIM_NOT_INSERTED);
    }
}

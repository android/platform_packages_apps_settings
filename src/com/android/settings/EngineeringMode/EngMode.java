/*
 * Copyright (C) 2009-2010 Broadcom Corporation
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
package com.android.settings.EngineeringMode;

import com.android.settings.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.os.SystemProperties;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.IccCardConstants;
import com.android.settings.EngineeringMode.*;
import android.util.Log;


/**
 * EngMode information menu item on the diagnostic screen
 */
public class EngMode extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static final String LOG_TAG = "EngineeringMode";
    public static final boolean DBG = true;

    public ListPreference mRefreshTime;
    private static int mRefreshTimeValue;
    private CheckBoxPreference mCMAS;
    private static final String CMAS_PROPERTY = TelephonyProperties.PROPERTY_CMAS_ENABLE;
    private ListPreference mDataStallTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.engineering_mode);
        PreferenceScreen prefSet = getPreferenceScreen();

        mRefreshTime = (ListPreference)prefSet.findPreference("EngineeringMode_RefreshTime_key");
        mCMAS = (CheckBoxPreference) findPreference("CMAS_ENABLE");

        int CMASValue = SystemProperties.getInt(CMAS_PROPERTY, 0);
        if(CMASValue==1)
        {
           mCMAS.setChecked(true);
        }

        mRefreshTime.setOnPreferenceChangeListener(this);

        //default set to 0 Sec
        mRefreshTimeValue=0;
        mRefreshTime.setValue("0");

        // data stall timer
        mDataStallTimer = (ListPreference) prefSet.findPreference("data_stall_timer");

        int dataStallTimerMsValue = Global.getInt(getContentResolver(),
                Global.DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS, 0);

        switch (dataStallTimerMsValue) {
            case 0:
                mDataStallTimer.setValue("0");
                break;
            case (1000 * 60):
                mDataStallTimer.setValue("1");
                break;
            case (1000 * 60 * 6):
                mDataStallTimer.setValue("2");
                break;
        }

        mDataStallTimer.setOnPreferenceChangeListener(this);
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on .
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mRefreshTime) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mRefreshTime.setValue((String) objValue);

            mRefreshTimeValue = Integer.valueOf((String) objValue).intValue();
            log ("Engineering Mode: mRefreshTimeValue = " +  mRefreshTimeValue);

        } else if (preference == mDataStallTimer) {
            log("Engineering Mode: data stall timer = " + (String) objValue);

            switch (Integer.valueOf((String) objValue).intValue()) {
                case 0:  // disable, 0 screen on, 0 screen off
                    Global.putInt(getContentResolver(),
                            Global.DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS,
                            0);
                    Global.putInt(getContentResolver(),
                            Global.DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS,
                            0);
                    break;
                case 1:  // 1 min, 1000 * 60 screen on, 1000 * 60 * 6 screen off
                    Global.putInt(getContentResolver(),
                            Global.DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS,
                            1000 * 60);
                    Global.putInt(getContentResolver(),
                            Global.DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS,
                            1000 * 60 * 6);
                    break;
                case 2:  // 6 min, 1000 * 60 * 6 screen on, 1000 * 60 * 12 screen off
                    Global.putInt(getContentResolver(),
                            Global.DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS,
                            1000 * 60 * 6);
                    Global.putInt(getContentResolver(),
                            Global.DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS,
                            1000 * 60 * 12);
                    break;
                default:
                    log("Engineering Mode: value not supported");
            }

        }
        // always let the preference setting proceed.
        return true;
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
         if (preference == mCMAS) {
            log (" onPreferenceTreeClick CMAS");

            int CMASValue;
            if(mCMAS.isChecked())
            {
               SystemProperties.set(CMAS_PROPERTY, "1");
            }else{
               SystemProperties.set(CMAS_PROPERTY, "0");
            }

            CMASValue = SystemProperties.getInt(CMAS_PROPERTY, 0);
            if(CMASValue==1)
            {
               log (" onPreferenceTreeClick CMASValue==1");
               mCMAS.setChecked(true);
            }else {
               log (" onPreferenceTreeClick CMASValue==0");
               mCMAS.setChecked(false);
            }
            return true;
         }else{
            log (" onPreferenceTreeClick Others");
            return false;
         }

    }

    static int getRefreshTime() {
       return mRefreshTimeValue;
    }

    static void log(String msg) {
        if(DBG) Log.d(LOG_TAG, msg);
    }
}

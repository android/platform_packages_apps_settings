/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RILConstants.SimCardID;
import com.android.internal.telephony.TelephonyProperties;

import com.android.settings.Utils;

public class AirplaneModeEnablerBrcm implements Preference.OnPreferenceChangeListener {
    private static final String LOG_TAG = "AirplaneModeEnablerBrcm";

    private static final boolean DBG = true;

    private final Context mContext;

    private PhoneStateIntentReceiver mPhoneStateReceiver;

    private PhoneStateListener mPhoneStateListener;

    private PhoneStateListener mPhoneStateListener2;

    private final CheckBoxPreference mCheckBoxPref;

    private static final int EVENT_SERVICE_STATE_CHANGED = 3;

    private static final int EVENT_CHECK_AIRPLANE_MODE_STATUS_ON = 4;

    private static final int EVENT_CHECK_AIRPLANE_MODE_STATUS_OFF = 5;

    private static final int DELAY_TIME_TO_CHECK_AIRPLANE_MODE_STATUS = 30*1000;

    private boolean isChanging = false;

    private boolean isChanging_SIM1 = false;

    private boolean isChanging_SIM2 = false;

    private int mServiceState[] = {ServiceState.STATE_OUT_OF_SERVICE, ServiceState.STATE_OUT_OF_SERVICE};

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    if(DBG) Log.d(LOG_TAG,"EVENT_SERVICE_STATE_CHANGED, service state:" + mPhoneStateReceiver.getServiceState().getState() + ", isChanging:" + isChanging);
                    //onAirplaneModeChanged();
                    break;
                case EVENT_CHECK_AIRPLANE_MODE_STATUS_ON:
                    if(DBG) Log.d(LOG_TAG,"EVENT_CHECK_AIRPLANE_MODE_STATUS_ON, airplane mode status:" + isAirplaneModeOn(mContext));
                    if(isChanging && isAirplaneModeOn(mContext)) {
                        if(DBG) Log.d(LOG_TAG,"EVENT_CHECK_AIRPLANE_MODE_STATUS_ON, airplane mode enable failed");
                        Toast.makeText(mContext, R.string.airplane_mode_switch_failed, Toast.LENGTH_LONG).show();
                        onAirplaneModeChanged();
                    }
                    break;
                case EVENT_CHECK_AIRPLANE_MODE_STATUS_OFF:
                    if(DBG) Log.d(LOG_TAG,"EVENT_CHECK_AIRPLANE_MODE_STATUS_OFF, airplane mode status:" + isAirplaneModeOn(mContext));
                    if(isChanging && !isAirplaneModeOn(mContext)) {
                        if(DBG) Log.d(LOG_TAG,"EVENT_CHECK_AIRPLANE_MODE_STATUS_OFF, airplane mode disable failed");
                        Toast.makeText(mContext, R.string.airplane_mode_switch_failed, Toast.LENGTH_LONG).show();
                        onAirplaneModeChanged();
                    }
                    break;
            }
        }
    };

    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if(DBG) Log.d(LOG_TAG,"mAirplaneModeObserver -> onChange(), isChanging:" + isChanging);
            if(!isChanging) {
                onAirplaneModeChanged();
            }
        }
    };

    private TelephonyManager mTelephonyManager;
    private TelephonyManager mTelephonyManager2;

    private void addPhoneStateListener(){
        if(mPhoneStateListener == null){
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    updateServiceState(serviceState);
                }
            };
            mTelephonyManager.listen(mPhoneStateListener,
                      PhoneStateListener.LISTEN_SERVICE_STATE);
        }
        if(mPhoneStateListener2 == null){
            mPhoneStateListener2 = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    updateServiceState2(serviceState);
                }
            };
            /* dual sim*/
            if (Utils.isSupportDualSim()) {
                mTelephonyManager2.listen(mPhoneStateListener2,
                         PhoneStateListener.LISTEN_SERVICE_STATE);
            }
        }
    }

    public AirplaneModeEnablerBrcm(Context context, CheckBoxPreference airplaneModeCheckBoxPreference) {

        mContext = context;
        mCheckBoxPref = airplaneModeCheckBoxPreference;

        airplaneModeCheckBoxPreference.setPersistent(false);

        mPhoneStateReceiver = new PhoneStateIntentReceiver(mContext, mHandler);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);

        mTelephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        /* dual sim */
        if (Utils.isSupportDualSim()) {
            mTelephonyManager2 = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE2);
        }
    }

    public void resume() {
        addPhoneStateListener();

        if(isChanging) {
            if(DBG) Log.d(LOG_TAG,"resume():The airplane mode is changing, set the airplane mode checkbox as disable");
            mCheckBoxPref.setEnabled(false);
        } else {
            if(DBG) Log.d(LOG_TAG,"resume():Set the airplane mode checkbox as enable");
            // This is the widget enabled state, not the preference toggled state
            mCheckBoxPref.setEnabled(true);
            mCheckBoxPref.setChecked(isAirplaneModeOn(mContext));
        }

        mPhoneStateReceiver.registerIntent();
        mCheckBoxPref.setOnPreferenceChangeListener(this);
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true,
                mAirplaneModeObserver);
    }

    public void pause() {
        mPhoneStateReceiver.unregisterIntent();
        mCheckBoxPref.setOnPreferenceChangeListener(null);
        mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private static boolean isOnlySIM1On(Context context) {
        return (Settings.Global.getInt(context.getContentResolver(),Settings.Global.PHONE1_ON, 1) != 0) &&
            (Settings.Global.getInt(context.getContentResolver(),Settings.Global.PHONE2_ON, 1) != 1);
    }

    private static boolean isOnlySIM2On(Context context) {
        return (Settings.Global.getInt(context.getContentResolver(),Settings.Global.PHONE1_ON, 1) != 1) &&
            (Settings.Global.getInt(context.getContentResolver(),Settings.Global.PHONE2_ON, 1) != 0);

    }

    private static boolean isBothSIMOn(Context context) {
        return (Settings.Global.getInt(context.getContentResolver(),Settings.Global.PHONE1_ON, 1) != 0) &&
            (Settings.Global.getInt(context.getContentResolver(),Settings.Global.PHONE2_ON, 1) != 0);

    }
    private void setAirplaneModeOn(boolean enabling) {
        if(DBG) Log.d(LOG_TAG,"setAirplaneModeOn(), enabling:" + enabling+ ", isChanging:" + isChanging);

        mCheckBoxPref.setEnabled(false);
        mCheckBoxPref.setSummary(enabling ? R.string.airplane_mode_turning_on
                : R.string.airplane_mode_turning_off);
        isChanging = true;
        if(isOnlySIM1On(mContext) || isBothSIMOn(mContext)) {
            isChanging_SIM1 = true;
        }
        if (Utils.isSupportDualSim()) {
            if(isOnlySIM2On(mContext) || isBothSIMOn(mContext)) {
                isChanging_SIM2 = true;
            }
        }

        if(enabling) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_CHECK_AIRPLANE_MODE_STATUS_ON),DELAY_TIME_TO_CHECK_AIRPLANE_MODE_STATUS);
        } else {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_CHECK_AIRPLANE_MODE_STATUS_OFF),DELAY_TIME_TO_CHECK_AIRPLANE_MODE_STATUS);
        }

        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                                enabling ? 1 : 0);
        // Update the UI to reflect system setting
        mCheckBoxPref.setChecked(enabling);

        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     * TODO: We update the checkbox summary when we get notified
     * that mobile radio is powered up/down. We should not have dependency
     * on one radio alone. We need to do the following:
     * - handle the case of wifi/bluetooth failures
     * - mobile does not send failure notification, fail on timeout.
     */
    private void onAirplaneModeChanged() {
        isChanging = false;
        boolean airplaneModeEnabled = isAirplaneModeOn(mContext);

        if(airplaneModeEnabled) {
            if(mHandler.hasMessages(EVENT_CHECK_AIRPLANE_MODE_STATUS_ON)) {
                mHandler.removeMessages(EVENT_CHECK_AIRPLANE_MODE_STATUS_ON);
            }
        } else {
            if(mHandler.hasMessages(EVENT_CHECK_AIRPLANE_MODE_STATUS_OFF)) {
                mHandler.removeMessages(EVENT_CHECK_AIRPLANE_MODE_STATUS_OFF);
            }
        }

        mCheckBoxPref.setChecked(airplaneModeEnabled);

        mCheckBoxPref.setSummary(airplaneModeEnabled ? null : "");
        mCheckBoxPref.setEnabled(true);
        if(DBG) Log.d(LOG_TAG,"onAirplaneModeChanged(), airplaneModeEnabled:" + airplaneModeEnabled);
    }

    /**
     * Called when someone clicks on the checkbox preference.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode, do not update database at this point
        } else {
            setAirplaneModeOn((Boolean) newValue);
        }
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // update summary
            onAirplaneModeChanged();
        }
    }

    private void updateServiceState(ServiceState serviceState) {
        int state = serviceState.getState();
        mServiceState[SimCardID.ID_ZERO.toInt()] = state;

        if(DBG) Log.d(LOG_TAG,"updateServiceState(), state:" + state + ", SIM1 is PRESENT:" + mTelephonyManager.hasIccCard());

        if(isChanging && isAirplaneModeOn(mContext)) { //Enabling airplane mode
            if(state == ServiceState.STATE_POWER_OFF) {
                isChanging_SIM1 = false;
            }
        } else if(isChanging && !isAirplaneModeOn(mContext)) { //Disabling airplane mode
            if(mTelephonyManager.hasIccCard()) {
                if(state == ServiceState.STATE_IN_SERVICE) {
                    isChanging_SIM1 = false;
                } else if(state == ServiceState.STATE_POWER_OFF && isOnlySIM2On(mContext)) {
                    isChanging_SIM1 = false;
                }
            } else {
                if(state == ServiceState.STATE_OUT_OF_SERVICE || state == ServiceState.STATE_POWER_OFF) {
                    isChanging_SIM1 = false;
                }
            }
        }
        if(DBG) Log.d(LOG_TAG,"updateServiceState(), isChanging:" + isChanging + ", isChanging_SIM1:" + isChanging_SIM1 + ", isChanging_SIM2:" + isChanging_SIM2);
        if(isChanging && !isChanging_SIM1 && ((Utils.isSupportDualSim())? !isChanging_SIM2 : true)) {
            onAirplaneModeChanged();
        }
    }

    private void updateServiceState2(ServiceState serviceState) {
        int state = serviceState.getState();
        mServiceState[SimCardID.ID_ONE.toInt()] = state;

        if(DBG) Log.d(LOG_TAG,"updateServiceState2(), state:" + state + ", SIM2 is PRESENT:" + mTelephonyManager2.hasIccCard());

        if(isChanging && isAirplaneModeOn(mContext)) { //Enabling airplane mode
            if(state == ServiceState.STATE_POWER_OFF) {
                isChanging_SIM2 = false;
            }
        } else if(isChanging && !isAirplaneModeOn(mContext)) { //Disabling airplane mode
            if(mTelephonyManager2.hasIccCard()) {
                if(state == ServiceState.STATE_IN_SERVICE) {
                    isChanging_SIM2 = false;
                } else if(state == ServiceState.STATE_POWER_OFF && isOnlySIM1On(mContext)) {
                    isChanging_SIM2 = false;
                }
            } else {
                if(state == ServiceState.STATE_OUT_OF_SERVICE || state == ServiceState.STATE_POWER_OFF) {
                    isChanging_SIM2 = false;
                }
            }
        }

        if(DBG) Log.d(LOG_TAG,"updateServiceState2(), isChanging:" + isChanging + ", isChanging_SIM1:" + isChanging_SIM1 + ", isChanging_SIM2:" + isChanging_SIM2);
        if(isChanging && !isChanging_SIM1 && !isChanging_SIM2) {
            onAirplaneModeChanged();
        }
    }

}

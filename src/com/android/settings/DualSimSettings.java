/*
 * Copyright (c) 2010, Code Aurora Forum. All rights reserved.
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


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Message;
import android.os.Handler;
import android.os.AsyncResult;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.PreferenceScreen;

import com.android.internal.telephony.ProxyManager;
import com.android.internal.telephony.PhoneFactory;

import com.android.settings.R;

public class DualSimSettings extends PreferenceActivity implements DialogInterface.OnDismissListener,
        DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener  {
    private static final String TAG = "DualSimSettings";

    private static final String KEY_VOICE = "voice";
    private static final String KEY_DATA = "data";
    private static final String KEY_SMS = "sms";
    private static final String KEY_CONFIG_SUB = "config_sub";

    private static final String CONFIG_SUB = "CONFIG_SUB";

    private static final int DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS = 100;

    static final int EVENT_SET_DATA_SUBSCRIPTION_DONE = 1;

    static final int SUBSCRIPTION_ID_0 = 0;
    static final int SUBSCRIPTION_ID_1 = 1;
    static final int SUBSCRIPTION_ID_INVALID = -1;

    private ListPreference mVoice;
    private ListPreference mData;
    private ListPreference mSms;
    private PreferenceScreen mConfigSub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.dual_sim_settings);

        mVoice = (ListPreference) findPreference(KEY_VOICE);
        mVoice.setOnPreferenceChangeListener(this);
        mData = (ListPreference) findPreference(KEY_DATA);
        mData.setOnPreferenceChangeListener(this);
        mSms = (ListPreference) findPreference(KEY_SMS);
        mSms.setOnPreferenceChangeListener(this);
        mConfigSub = (PreferenceScreen) findPreference(KEY_CONFIG_SUB);
        mConfigSub.getIntent().putExtra(CONFIG_SUB, true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateState();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void updateState() {
        updateVoiceSummary();
        updateDataSummary();
        updateSmsSummary();
    }

     private void updateVoiceSummary() {
        int Voice_val = SUBSCRIPTION_ID_INVALID;
        CharSequence[] summaries = getResources().getTextArray(R.array.dualsim_summaries);

        try {
            Voice_val = Settings.System.getInt(getContentResolver(),Settings.System.DUAL_SIM_VOICE_CALL);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Dual sim Voice Call Values", snfe);
        }

        Log.d(TAG, "updateVoiceSummary: Voice_val = " + Voice_val);
        if (Voice_val == SUBSCRIPTION_ID_0) {
            mVoice.setValue("0");
            mVoice.setSummary(summaries[0]);
        } else if (Voice_val == SUBSCRIPTION_ID_1) {
            mVoice.setValue("1");
            mVoice.setSummary(summaries[1]);
        } else {
            mVoice.setValue("0");
            mVoice.setSummary(summaries[0]);
        }
    }

    private void updateDataSummary() {
        int Data_val = SUBSCRIPTION_ID_INVALID;
        CharSequence[] summaries = getResources().getTextArray(R.array.dualsim_summaries);

        try {
            Data_val = Settings.System.getInt(getContentResolver(),Settings.System.DUAL_SIM_DATA_CALL);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Dual Sim Data Subscription Value.", snfe);
        }

        Log.d(TAG, "updateDataSummary: Data_val = " + Data_val);
        if (Data_val == SUBSCRIPTION_ID_0) {
            mData.setValue("0");
            mData.setSummary(summaries[0]);
        } else if (Data_val == SUBSCRIPTION_ID_1) {
            mData.setValue("1");
            mData.setSummary(summaries[1]);
        } else {
            mData.setValue("0");
            mData.setSummary(summaries[0]);
        }
    }

    private void updateSmsSummary() {
        int Sms_val = SUBSCRIPTION_ID_INVALID;
        CharSequence[] summaries = getResources().getTextArray(R.array.dualsim_summaries);

        try {
            Sms_val = Settings.System.getInt(getContentResolver(),Settings.System.DUAL_SIM_SMS);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Dual Sim SMS Call Values.", snfe);
        }

        Log.d(TAG, "updateSmsSummary: Sms_val = " + Sms_val);
        if (Sms_val == SUBSCRIPTION_ID_0) {
            mSms.setValue("0");
            mSms.setSummary(summaries[0]);
        } else if (Sms_val == SUBSCRIPTION_ID_1) {
            mSms.setValue("1");
            mSms.setSummary(summaries[1]);
        } else {
            mSms.setValue("0");
            mSms.setSummary(summaries[0]);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        CharSequence[] summaries = getResources().getTextArray(R.array.dualsim_summaries);

        if (KEY_VOICE.equals(key)) {
            int V_value = Integer.parseInt((String) objValue);
            Log.d(TAG, "setVoiceSubscription " + V_value);
            PhoneFactory.setVoiceSubscription(V_value);
            mVoice.setSummary(summaries[V_value]);
        }

        if (KEY_DATA.equals(key)) {
            int D_value = Integer.parseInt((String) objValue);
            Log.d(TAG, "setDataSubscription " + D_value);
            showDialog(DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS);

            ProxyManager mProxyManager = ProxyManager.getInstance();
            Message setDdsMsg = Message.obtain(mHandler, EVENT_SET_DATA_SUBSCRIPTION_DONE, null);
            mProxyManager.setDataSubscription(D_value, setDdsMsg);
        }

        if (KEY_SMS.equals(key)) {
            int S_value = Integer.parseInt((String) objValue);
            Log.d(TAG, "setSMSSubscription " + S_value);
            PhoneFactory.setSMSSubscription(S_value);
            mSms.setSummary(summaries[S_value]);
        }

        return true;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;

            switch(msg.what) {
                case EVENT_SET_DATA_SUBSCRIPTION_DONE:
                    Log.d(TAG, "EVENT_SET_DATA_SUBSCRIPTION_DONE");
                    dismissDialog(DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS);
                    getPreferenceScreen().setEnabled(true);
                    updateDataSummary();

                    ar = (AsyncResult) msg.obj;

                    String status;

                    if (ar.exception != null) {
                        // This should never happens.  But display an alert message in case.
                        status = getResources().getString(R.string.set_dds_failed);
                        displayAlertDialog(status);
                        break;
                    }

                    final ProxyManager.SetDdsResult result = (ProxyManager.SetDdsResult)ar.result;

                    Log.d(TAG, "SET_DATA_SUBSCRIPTION_DONE: result = " + result.toString());

                    if (result == ProxyManager.SetDdsResult.SUCCESS) {
                        status = getResources().getString(R.string.set_dds_success);
                        Toast toast = Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        status = getResources().getString(R.string.set_dds_failed);
                        displayAlertDialog(status);
                    }

                    break;
            }
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS) {
            ProgressDialog dialog = new ProgressDialog(this);

            dialog.setMessage(getResources().getString(R.string.set_data_subscription_progress));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);

            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS) {
            // when the dialogs come up, we'll need to indicate that
            // we're in a busy state to disallow further input.
            getPreferenceScreen().setEnabled(false);
        }
    }

    // This is a method implemented for DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialog) {
        Log.d(TAG, "onDismiss!");
    }

    // This is a method implemented for DialogInterface.OnClickListener.
    public void onClick(DialogInterface dialog, int which) {
        Log.d(TAG, "onClick!");
    }

    void displayAlertDialog(String msg) {
        Log.d(TAG, "displayErrorDialog!" + msg);
        new AlertDialog.Builder(this).setMessage(msg)
               .setTitle(android.R.string.dialog_alert_title)
               .setIcon(android.R.drawable.ic_dialog_alert)
               .setPositiveButton(android.R.string.yes, this)
               .show()
               .setOnDismissListener(this);
    }
}


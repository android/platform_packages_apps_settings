/* Copyright (c) 2011-12, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Code Aurora Forum, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
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

import com.android.internal.telephony.msim.Subscription.SubscriptionStatus;
import com.android.internal.telephony.msim.SubscriptionManager;
import com.android.internal.telephony.msim.MSimPhoneFactory;

import com.android.settings.R;

public class MultiSimSettings extends PreferenceActivity implements DialogInterface.
        OnDismissListener, DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener  {
    private static final String TAG = "MultiSimSettings";

    private static final String KEY_VOICE = "voice";
    private static final String KEY_DATA = "data";
    private static final String KEY_SMS = "sms";
    private static final String KEY_CONFIG_SUB = "config_sub";

    private static final String CONFIG_SUB = "CONFIG_SUB";

    private static final int DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS = 100;

    static final int EVENT_SET_DATA_SUBSCRIPTION_DONE = 1;
    static final int EVENT_SUBSCRIPTION_ACTIVATED = 2;
    static final int EVENT_SUBSCRIPTION_DEACTIVATED = 3;
    static final int EVENT_SET_VOICE_SUBSCRIPTION = 4;
    static final int EVENT_SET_SMS_SUBSCRIPTION = 5;
    protected boolean mIsForeground = false;
    static final int SUBSCRIPTION_ID_0 = 0;
    static final int SUBSCRIPTION_ID_1 = 1;
    static final int SUBSCRIPTION_ID_INVALID = -1;
    static final int PROMPT_OPTION = 2;
    static final int SUBSCRIPTION_DUAL_STANDBY = 2;

    private ListPreference mVoice;
    private ListPreference mData;
    private ListPreference mSms;
    private PreferenceScreen mConfigSub;

    SubscriptionManager subManager = SubscriptionManager.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.multi_sim_settings);

        mVoice = (ListPreference) findPreference(KEY_VOICE);
        mVoice.setOnPreferenceChangeListener(this);
        mData = (ListPreference) findPreference(KEY_DATA);
        mData.setOnPreferenceChangeListener(this);
        mSms = (ListPreference) findPreference(KEY_SMS);
        mSms.setOnPreferenceChangeListener(this);
        mConfigSub = (PreferenceScreen) findPreference(KEY_CONFIG_SUB);
        mConfigSub.getIntent().putExtra(CONFIG_SUB, true);
        if (isAirplaneModeOn()) {
            Log.d(TAG, "Airplane mode is ON, grayout the config subscription menu!!!");
            mConfigSub.setEnabled(false);
        }
        for (int subId = 0; subId < SubscriptionManager.NUM_SUBSCRIPTIONS; subId++) {
            subManager.registerForSubscriptionActivated(subId,
                    mHandler, EVENT_SUBSCRIPTION_ACTIVATED, null);
            subManager.registerForSubscriptionDeactivated(subId,
                    mHandler, EVENT_SUBSCRIPTION_DEACTIVATED, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        int count = subManager.getActiveSubscriptionsCount();
        if (count == SUBSCRIPTION_DUAL_STANDBY) {
            mVoice.setEntries(R.array.multi_sim_entries_voice);
            mVoice.setEntryValues(R.array.multi_sim_values_voice);
        } else  {
            mVoice.setEntries(R.array.multi_sim_entries_voice_without_prompt);
            mVoice.setEntryValues(R.array.multi_sim_values_voice_without_prompt);
        }
        mIsForeground = true;
        updateState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
    }

    private boolean isAirplaneModeOn() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void updateState() {
        updateVoiceSummary();
        updateDataSummary();
        updateSmsSummary();
    }

    private void updateVoiceSummary() {
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries_voice);
        int voiceSub = MSimPhoneFactory.getVoiceSubscription();
        boolean promptEnabled  = MSimPhoneFactory.isPromptEnabled();
        int count = subManager.getActiveSubscriptionsCount();

        Log.d(TAG, "updateVoiceSummary: voiceSub =  " + voiceSub
                + " promptEnabled = " + promptEnabled
                + " number of active SUBs = " + count);

        if (promptEnabled && count == SUBSCRIPTION_DUAL_STANDBY) {
            Log.d(TAG, "prompt is enabled: setting value to : 2");
            mVoice.setValue("2");
            mVoice.setSummary(summaries[2]);
        } else {
            String sub = Integer.toString(voiceSub);
            Log.d(TAG, "setting value to : " + sub);
            mVoice.setValue(sub);
            mVoice.setSummary(summaries[voiceSub]);
        }
    }

    private void updateDataSummary() {
        int Data_val = SUBSCRIPTION_ID_INVALID;
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries);

        try {
            Data_val = Settings.System.getInt(getContentResolver(),
                    Settings.System.MULTI_SIM_DATA_CALL_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Multi Sim Data Subscription Value.", snfe);
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
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries);

        try {
            Sms_val = Settings.System.getInt(getContentResolver(),
                    Settings.System.MULTI_SIM_SMS_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Multi Sim SMS Call Values.", snfe);
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
        String status;
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries);

        if (KEY_VOICE.equals(key)) {
            summaries = getResources().getTextArray(R.array.multi_sim_summaries_voice);
            int voiceSub = Integer.parseInt((String) objValue);
            if (voiceSub == PROMPT_OPTION) {
                MSimPhoneFactory.setPromptEnabled(true);
                mVoice.setSummary(summaries[voiceSub]);
                Log.d(TAG, "prompt is enabled " + voiceSub);
            } else if (subManager.getCurrentSubscription(voiceSub).subStatus
                   == SubscriptionStatus.SUB_ACTIVATED) {
                Log.d(TAG, "setVoiceSubscription " + voiceSub);
                MSimPhoneFactory.setPromptEnabled(false);
                MSimPhoneFactory.setVoiceSubscription(voiceSub);
                mVoice.setSummary(summaries[voiceSub]);
            } else {
                status = getResources().getString(R.string.set_voice_error);
                displayAlertDialog(status);
            }
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_SET_VOICE_SUBSCRIPTION));
        }

        if (KEY_DATA.equals(key)) {
            int dataSub = Integer.parseInt((String) objValue);
            Log.d(TAG, "setDataSubscription " + dataSub);
            if (mIsForeground) {
                showDialog(DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS);
            }
            SubscriptionManager mSubscriptionManager = SubscriptionManager.getInstance();
            Message setDdsMsg = Message.obtain(mHandler, EVENT_SET_DATA_SUBSCRIPTION_DONE, null);
            mSubscriptionManager.setDataSubscription(dataSub, setDdsMsg);
        }

        if (KEY_SMS.equals(key)) {
            int smsSub = Integer.parseInt((String) objValue);
            Log.d(TAG, "setSMSSubscription " + smsSub);
            if (subManager.getCurrentSubscription(smsSub).subStatus
                    == SubscriptionStatus.SUB_ACTIVATED) {
                MSimPhoneFactory.setSMSSubscription(smsSub);
                mSms.setSummary(summaries[smsSub]);
            } else {
                status = getResources().getString(R.string.set_sms_error);
                displayAlertDialog(status);
            }
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_SET_SMS_SUBSCRIPTION));
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
                    if (mIsForeground) {
                        dismissDialog(DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS);
                    }
                    getPreferenceScreen().setEnabled(true);
                    updateDataSummary();

                    ar = (AsyncResult) msg.obj;

                    String status;

                    if (ar.exception != null) {
                        status = getResources().getString(R.string.set_dds_error)
                                    + " " + ar.exception.getMessage();
                        displayAlertDialog(status);
                        break;
                    }

                    boolean result = (Boolean)ar.result;

                    Log.d(TAG, "SET_DATA_SUBSCRIPTION_DONE: result = " + result);

                    if (result == true) {
                        status = getResources().getString(R.string.set_dds_success);
                        Toast toast = Toast.makeText(getApplicationContext(), status,
                                Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        status = getResources().getString(R.string.set_dds_failed);
                        displayAlertDialog(status);
                    }

                    break;
                case EVENT_SUBSCRIPTION_ACTIVATED:
                case EVENT_SUBSCRIPTION_DEACTIVATED:
                    int count = subManager.getActiveSubscriptionsCount();
                    if (count == SUBSCRIPTION_DUAL_STANDBY) {
                        mVoice.setEntries(R.array.multi_sim_entries_voice);
                        mVoice.setEntryValues(R.array.multi_sim_values_voice);
                    } else  {
                        mVoice.setEntries(R.array.multi_sim_entries_voice_without_prompt);
                        mVoice.setEntryValues(R.array.multi_sim_values_voice_without_prompt);
                    }
                    break;
                case EVENT_SET_VOICE_SUBSCRIPTION:
                    updateVoiceSummary();
                    break;
                case EVENT_SET_SMS_SUBSCRIPTION:
                    updateSmsSummary();
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
        if (!mIsForeground) {
            Log.d(TAG, "The activitiy is not in foreground. Do not display dialog!!!");
            return;
        }
        Log.d(TAG, "displayErrorDialog!" + msg);
        new AlertDialog.Builder(this).setMessage(msg)
               .setTitle(android.R.string.dialog_alert_title)
               .setIcon(android.R.drawable.ic_dialog_alert)
               .setPositiveButton(android.R.string.yes, this)
               .show()
               .setOnDismissListener(this);
    }
}


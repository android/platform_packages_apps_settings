/* Copyright (c) 2011-13, The Linux Foundation. All rights reserved.
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
 *     * Neither the name of The Linux Foundation nor the names of its
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


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.os.AsyncResult;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.MSimTelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.codeaurora.telephony.msim.MSimPhoneFactory;
import com.codeaurora.telephony.msim.Subscription.SubscriptionStatus;
import com.codeaurora.telephony.msim.SubscriptionManager;


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
    static final int SUBSCRIPTION_ID_INVALID = -1;
    static final int SUBSCRIPTION_DUAL_STANDBY = 2;

    private ListPreference mVoice;
    private ListPreference mData;
    private ListPreference mSms;
    private PreferenceScreen mConfigSub;
    private int mNumPhones = MSimTelephonyManager.getDefault().getPhoneCount();
    private CharSequence[] entries; // Used for entries like Subscription1, Subscription2 ...
    private CharSequence[] entryValues; // Used for entryValues like 0, 1 ,2 ...
    private CharSequence[] summaries; // Used for Summaries like Aubscription1, Subscription2....
    private CharSequence[] entriesPrompt; // Used in case of prompt option is required.
    private CharSequence[] entryValuesPrompt; // Used in case of prompt option is required.
    private CharSequence[] summariesPrompt; // Used in case of prompt option is required.

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
        for (int subId = 0; subId < SubscriptionManager.NUM_SUBSCRIPTIONS; subId++) {
            subManager.registerForSubscriptionActivated(subId,
                    mHandler, EVENT_SUBSCRIPTION_ACTIVATED, null);
            subManager.registerForSubscriptionDeactivated(subId,
                    mHandler, EVENT_SUBSCRIPTION_DEACTIVATED, null);
        }

        // Create and Intialize the strings required for MultiSIM
        // Dynamic creation of entries instead of using static array vlues.
        // entries are Subscription1, Subscription2, Subscription3 ....
        // EntryValues are 0, 1 ,2 ....
        // Summaries are Subscription1, Subscription2, Subscription3 ....
        entries = new CharSequence[mNumPhones];
        entryValues = new CharSequence[mNumPhones];
        summaries = new CharSequence[mNumPhones];
        entriesPrompt = new CharSequence[mNumPhones + 1];
        entryValuesPrompt = new CharSequence[mNumPhones + 1];
        summariesPrompt = new CharSequence[mNumPhones + 1];
        CharSequence[] subString = getResources().getTextArray(R.array.multi_sim_entries);
        int i = 0;
        for (i = 0; i < mNumPhones; i++) {
            entries[i] = subString[i];
            summaries[i] = subString[i];
            summariesPrompt[i] = subString[i];
            entriesPrompt[i] = subString[i];
            entryValues[i] = Integer.toString(i);
            entryValuesPrompt[i] = Integer.toString(i);
        }
        entryValuesPrompt[i] = Integer.toString(i);
        entriesPrompt[i] = getResources().getString(R.string.prompt);
        summariesPrompt[i] = getResources().getString(R.string.prompt_user);
        IntentFilter intentFilter =
                new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(new AirplaneModeBroadcastReceiver(), intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAirplaneModeOn()) {
            Log.d(TAG, "Airplane mode is ON, grayout the config subscription menu!!!");
            mConfigSub.setEnabled(false);
        } else {
            mConfigSub.setEnabled(true);
        }
        updateMultiSimEntriesForVoice();
        updateMultiSimEntriesForData();
        updateMultiSimEntriesForSms();
        mIsForeground = true;
        updateState();
    }

    /**
     ** Receiver for Airplane mode changed intent broadcasts.
     **/
    private class AirplaneModeBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                Log.d(TAG, "Airplane mode is: " + isAirplaneModeOn());
                if (isAirplaneModeOn()) {
                    Log.d(TAG, "Airplane mode is ON, grayout the config subscription menu!!!");
                    mConfigSub.setEnabled(false);
                } else {
                    mConfigSub.setEnabled(true);
                }
            }
        }
    }

    protected void updateMultiSimEntriesForData() {
        mData.setEntries(entries);
        mData.setEntryValues(entryValues);
    }

    protected void updateMultiSimEntriesForSms() {
        int count = subManager.getActiveSubscriptionsCount();
        if (count >= SUBSCRIPTION_DUAL_STANDBY) {
            mSms.setEntries(entriesPrompt);
            mSms.setEntryValues(entryValuesPrompt);
        } else  {
            mSms.setEntries(entries);
            mSms.setEntryValues(entryValues);
        }
    }

    protected void updateMultiSimEntriesForVoice() {
        int count = subManager.getActiveSubscriptionsCount();
        if (count >= SUBSCRIPTION_DUAL_STANDBY) {
            mVoice.setEntries(entriesPrompt);
            mVoice.setEntryValues(entryValuesPrompt);
        } else  {
            mVoice.setEntries(entries);
            mVoice.setEntryValues(entryValues);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void updateState() {
        updateVoiceSummary();
        updateDataSummary();
        updateSmsSummary();
    }

    private void updateVoiceSummary() {

        int voiceSub = MSimPhoneFactory.getVoiceSubscription();
        boolean promptEnabled  = MSimPhoneFactory.isPromptEnabled();
        int count = subManager.getActiveSubscriptionsCount();

        Log.d(TAG, "updateVoiceSummary: voiceSub =  " + voiceSub
                + " promptEnabled = " + promptEnabled
                + " number of active SUBs = " + count);

        if (promptEnabled && count >= SUBSCRIPTION_DUAL_STANDBY) {

            Log.d(TAG, "prompt is enabled: setting value to : " + mNumPhones);
            mVoice.setValue(Integer.toString(mNumPhones));
            mVoice.setSummary(summariesPrompt[mNumPhones]);
        } else {
            String sub = Integer.toString(voiceSub);
            Log.d(TAG, "setting value to : " + sub);
            mVoice.setValue(sub);
            mVoice.setSummary(summaries[voiceSub]);
        }
    }

    private void updateDataSummary() {
        int dataSub = MSimPhoneFactory.getDataSubscription();

        Log.d(TAG, "updateDataSummary: Data Subscription : = " + dataSub);
        mData.setValue(Integer.toString(dataSub));
        mData.setSummary(summaries[dataSub]);
    }

    private void updateSmsSummary() {
        int smsSub = MSimPhoneFactory.getSMSSubscription();
        boolean promptEnabled  = MSimPhoneFactory.isSMSPromptEnabled();
        int count = subManager.getActiveSubscriptionsCount();

        Log.d(TAG, "updateSmsSummary: SmsSub =  " + smsSub
                + " promptEnabled = " + promptEnabled
                + " number of active SUBs = " + count);

        if (promptEnabled && count >= SUBSCRIPTION_DUAL_STANDBY) {
            Log.d(TAG, "prompt is enabled: setting value to : " + mNumPhones);
            mSms.setValue(Integer.toString(mNumPhones));
            mSms.setSummary(summariesPrompt[mNumPhones]);
        } else {
            String sub = Integer.toString(smsSub);
            Log.d(TAG, "setting value to : " + sub);
            mSms.setValue(sub);
            mSms.setSummary(summaries[smsSub]);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        String status;
        Log.d(TAG, "onPreferenceChange:::: " );

        if (KEY_VOICE.equals(key)) {

            int voiceSub = Integer.parseInt((String) objValue);

            if (voiceSub == mNumPhones) { //mNumPhones is the maximum index of the UI options.
                                         //This will be the Prompt option.
                MSimPhoneFactory.setPromptEnabled(true);
                mVoice.setSummary(summariesPrompt[voiceSub]);
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
            if (smsSub == mNumPhones) { //mNumPhones is the maximum index of the UI options.
                                         //This will be the Prompt option.
                MSimPhoneFactory.setSMSPromptEnabled(true);
                mSms.setSummary(summariesPrompt[smsSub]);
                Log.d(TAG, "prompt is enabled " + smsSub);
            } else if (subManager.getCurrentSubscription(smsSub).subStatus
                   == SubscriptionStatus.SUB_ACTIVATED) {
                Log.d(TAG, "setSMSSubscription " + smsSub);
                MSimPhoneFactory.setSMSPromptEnabled(false);
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
                    updateMultiSimEntriesForVoice();
                    updateMultiSimEntriesForSms();
                    break;
                case EVENT_SET_VOICE_SUBSCRIPTION:
                    updateVoiceSummary();
                    break;
                case EVENT_SET_SMS_SUBSCRIPTION:
                    updateSmsSummary();
                    break;
                default:
                    Log.w(TAG, "Unknown Event " + msg.what);
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


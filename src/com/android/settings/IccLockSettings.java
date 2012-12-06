/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;

import com.android.internal.telephony.RILConstants.SimCardID;
import android.app.Dialog;
import android.app.ProgressDialog;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.content.res.Configuration;
import android.util.Log;
import com.android.settings.Utils;

/**
 * Implements the preference screen to enable/disable ICC lock and
 * also the dialogs to change the ICC PIN. In the former case, enabling/disabling
 * the ICC lock will prompt the user for the current PIN.
 * In the Change PIN case, it prompts the user for old pin, new pin and new pin
 * again before attempting to change it. Calls the SimCard interface to execute
 * these operations.
 *
 */
public class IccLockSettings extends PreferenceActivity
        implements EditPinPreference.OnPinEnteredListener {
    private static final String TAG = "IccLockSettings";
    private static final boolean DBG = true;

    private static final String TAG = "IccLockSettings";
    private static final boolean DBG = false;

    private static final int OFF_MODE = 0;
    // State when enabling/disabling ICC lock
    private static final int ICC_LOCK_MODE = 1;
    // State when entering the old pin
    private static final int ICC_OLD_MODE = 2;
    // State when entering the new pin - first time
    private static final int ICC_NEW_MODE = 3;
    // State when entering the new pin - second time
    private static final int ICC_REENTER_MODE = 4;

    // State when entering the locked pin
    private static final int UNLOCK_PIN_MODE = 5;
    // State when entering the locked puk
    private static final int UNLOCK_PUK_MODE = 6;
    // State when entering the locked puk
    private static final int UNLOCK_PUK_ENTER_NEW_PIN_MODE = 7;
    // State when entering the locked puk
    private static final int UNLOCK_PUK_REENTER_NEW_PIN_MODE = 8;

    // Keys in xml file
    private static final String PIN_DIALOG = "sim_pin";
    private static final String PIN_TOGGLE = "sim_toggle";
    // Keys in icicle
    private static final String DIALOG_STATE = "dialogState";
    private static final String DIALOG_PIN = "dialogPin";
    private static final String DIALOG_ERROR = "dialogError";
    private static final String ENABLE_TO_STATE = "enableState";

    // Save and restore inputted PIN code when configuration changed
    // (ex. portrait<-->landscape) during change PIN code
    private static final String OLD_PINCODE = "oldPinCode";
    private static final String NEW_PINCODE = "newPinCode";

    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;
    // Which dialog to show next when popped up
    private int mDialogState = OFF_MODE;

    private String mPin;
    private String mOldPin;
    private String mNewPin;
    private String mError;
    private String mPuk;
    // Are we trying to enable or disable ICC lock?
    private boolean mToState;

    private Phone mPhone;

    private int mSimId;

    private EditPinPreference mPinDialog;
    private CheckBoxPreference mPinToggle;

    private Resources mRes;

    // For async handler to identify request type
    private static final int MSG_ENABLE_ICC_PIN_COMPLETE = 100;
    private static final int MSG_CHANGE_ICC_PIN_COMPLETE = 101;
    private static final int MSG_SIM_STATE_CHANGED = 102;

    // For replies from IccCard interface
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
                case MSG_ENABLE_ICC_PIN_COMPLETE:
                    iccLockChanged(ar.exception == null, msg.arg1);
                    break;
                case MSG_CHANGE_ICC_PIN_COMPLETE:
                    iccPinChanged(ar.exception == null, msg.arg1);
                    break;
                case MSG_SIM_STATE_CHANGED:
                    updatePreferences();
                    break;
            }

            return;
        }
    };

    private final BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SIM_STATE_CHANGED));
            }
        }
    };

    // For top-level settings screen to query
    static boolean isIccLockEnabled() {
        return PhoneFactory.getDefaultPhone().getIccCard().getIccLockEnabled();
    }

    static String getSummary(Context context) {
        Resources res = context.getResources();
        String summary = isIccLockEnabled()
                ? res.getString(R.string.sim_lock_on)
                : res.getString(R.string.sim_lock_off);
        return summary;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Utils.isMonkeyRunning()) {
            finish();
            return;
        }

        Intent intent = getIntent();
        mSimId = intent.getIntExtra(Intent.EXTRA_TEXT, SimCardID.ID_ZERO.toInt());

        addPreferencesFromResource(R.xml.sim_lock_settings);

        mPinDialog = (EditPinPreference) findPreference(PIN_DIALOG);
        mPinToggle = (CheckBoxPreference) findPreference(PIN_TOGGLE);
        if (savedInstanceState != null && savedInstanceState.containsKey(DIALOG_STATE)) {
            mDialogState = savedInstanceState.getInt(DIALOG_STATE);
            mPin = savedInstanceState.getString(DIALOG_PIN);
            mError = savedInstanceState.getString(DIALOG_ERROR);
            mToState = savedInstanceState.getBoolean(ENABLE_TO_STATE);

            // Restore inputted PIN code
            switch (mDialogState) {
                case ICC_NEW_MODE:
                    mOldPin = savedInstanceState.getString(OLD_PINCODE);
                    break;

                case ICC_REENTER_MODE:
                    mOldPin = savedInstanceState.getString(OLD_PINCODE);
                    mNewPin = savedInstanceState.getString(NEW_PINCODE);
                    break;

                case ICC_LOCK_MODE:
                case ICC_OLD_MODE:
                default:
                    break;
            }
        }

        if(DBG) Log.d(TAG,"onCreate(), SimCardID:" + mSimId + ", mToState:" + mToState);

        mPinDialog.setOnPinEnteredListener(this);

        // Don't need any changes to be remembered
        getPreferenceScreen().setPersistent(false);

        mPhone = PhoneFactory.getDefaultPhone(mSimId == SimCardID.ID_ONE.toInt() ? SimCardID.ID_ONE : SimCardID.ID_ZERO);

        if(!mPhone.getIccCard().getIccLockEnabled())
            mPinDialog.setEnabled(false);

        mRes = getResources();
        updatePreferences();
    }

    private void updatePreferences() {
        mPinToggle.setChecked(mPhone.getIccCard().getIccLockEnabled());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(DBG){
            Log.d(TAG,"onResume(), mToState:" + mToState);
            Log.d(TAG,"onResume(), mPhone.getIccCard().getIccLockEnabled():" + mPhone.getIccCard().getIccLockEnabled());
        }

        // ACTION_SIM_STATE_CHANGED is sticky, so we'll receive current state after this call,
        // which will call updatePreferences().
        final IntentFilter filter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        registerReceiver(mSimStateReceiver, filter);

        //Set title
        if(mSimId == SimCardID.ID_ONE.toInt()){
            setTitle(R.string.sim_2_lock_settings);
        }else{
            if (Utils.isSupportDualSim()) {
                setTitle(R.string.sim_1_lock_settings);
            } else {
                setTitle(R.string.sim_lock_settings);
            }
        }

        if (mDialogState != OFF_MODE) {
            showPinDialog();
        } else {
            // Prep for standard click on "Change PIN"
            resetDialogState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mSimStateReceiver);
        //Normally it should never be here to dismiss dialog.
        if (mSimUnlockProgressDialog != null && mSimUnlockProgressDialog.isShowing()) {
            mSimUnlockProgressDialog.dismiss();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Do nothing, just when mcc/mnc changed, do not recreate the activity, see manifest.xml
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        // Need to store this state for slider open/close
        // There is one case where the dialog is popped up by the preference
        // framework. In that case, let the preference framework store the
        // dialog state. In other cases, where this activity manually launches
        // the dialog, store the state of the dialog.
        if (mPinDialog.isDialogOpen()) {
            out.putInt(DIALOG_STATE, mDialogState);
            out.putString(DIALOG_PIN, mPinDialog.getEditText().getText().toString());
            out.putString(DIALOG_ERROR, mError);
            out.putBoolean(ENABLE_TO_STATE, mToState);

            // Save inputted PIN code
            switch (mDialogState) {
                case ICC_NEW_MODE:
                    out.putString(OLD_PINCODE, mOldPin);
                    break;

                case ICC_REENTER_MODE:
                    out.putString(OLD_PINCODE, mOldPin);
                    out.putString(NEW_PINCODE, mNewPin);
                    break;

                case ICC_LOCK_MODE:
                case ICC_OLD_MODE:
                default:
                    break;
            }
        } else {
            super.onSaveInstanceState(out);
        }
    }

    private void showPinDialog() {
        if (mDialogState == OFF_MODE) {
            return;
        }
        setDialogValues();

        mPinDialog.showPinDialog();
    }

    private void setDialogValues() {
        mPinDialog.setText(mPin);
        String message = "";
        int pinRemainingCount = getPinRemainingCount(0);
        int pukRemainingCount = getPinRemainingCount(1);
        switch (mDialogState) {
            case ICC_LOCK_MODE:
                message = mRes.getString(R.string.sim_enter_pin);
                message += "(" + Integer.toString(pinRemainingCount) + ")";
                mPinDialog.setDialogTitle(mToState
                        ? mRes.getString(R.string.sim_enable_sim_lock)
                        : mRes.getString(R.string.sim_disable_sim_lock));
                break;
            case ICC_OLD_MODE:
                message = mRes.getString(R.string.sim_enter_old);
                message += "(" + Integer.toString(pinRemainingCount) + ")";
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                mPinDialog.setTitle(mRes.getString(R.string.sim_pin_change));
                break;
            case ICC_NEW_MODE:
                message = mRes.getString(R.string.sim_enter_new);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
            case ICC_REENTER_MODE:
                message = mRes.getString(R.string.sim_reenter_new);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
            case UNLOCK_PIN_MODE:
                message = mRes.getString(R.string.sim_enter_pin);
                message += "(" + Integer.toString(pinRemainingCount) + ")";
                mPinDialog.setTitle(mRes.getString(R.string.sim_unlock_pin));
                break;
            case UNLOCK_PUK_MODE:
                message = mRes.getString(R.string.sim_enter_puk);
                message += "(" + Integer.toString(pukRemainingCount) + ")";
                mPinDialog.setTitle(mRes.getString(R.string.sim_unlock_puk));
                break;
            case UNLOCK_PUK_ENTER_NEW_PIN_MODE:
                message = mRes.getString(R.string.sim_enter_new);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
            case UNLOCK_PUK_REENTER_NEW_PIN_MODE:
                message = mRes.getString(R.string.sim_reenter_new);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
            default:
                break;
        }
        if (mError != null) {
            message = mError + "\n" + message;
            mError = null;
        }
        mPinDialog.setDialogMessage(message);
    }

    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
        if (!positiveResult) {
            resetDialogState();
            return;
        }

        mPin = preference.getText();
        if (!reasonablePin(mPin)) {
            // inject error message and display dialog again
            mError = mRes.getString(R.string.sim_bad_pin);
            showPinDialog();
            return;
        }
        switch (mDialogState) {
            case ICC_LOCK_MODE:
                //tryChangeIccLockState();
                tryUnlockSimIfNeededAndChangeIccLockState();
                break;
            case ICC_OLD_MODE:
                mOldPin = mPin;
                mDialogState = ICC_NEW_MODE;
                mError = null;
                mPin = null;
                showPinDialog();
                break;
            case ICC_NEW_MODE:
                mNewPin = mPin;
                mDialogState = ICC_REENTER_MODE;
                mPin = null;
                showPinDialog();
                break;
            case ICC_REENTER_MODE:
                if (!mPin.equals(mNewPin)) {
                    mError = mRes.getString(R.string.sim_pins_dont_match);
                    mDialogState = ICC_NEW_MODE;
                    mPin = null;
                    showPinDialog();
                } else {
                    mError = null;
                    tryChangePin();
                }
                break;
            case UNLOCK_PIN_MODE:
                unlockPin();
                break;
            case UNLOCK_PUK_MODE:
                mPuk = mPin;
                mDialogState = UNLOCK_PUK_ENTER_NEW_PIN_MODE;
                mError = null;
                mPin = null;
                showPinDialog();
                break;
            case UNLOCK_PUK_ENTER_NEW_PIN_MODE:
                mNewPin = mPin;
                mDialogState = UNLOCK_PUK_REENTER_NEW_PIN_MODE;
                mPin = null;
                showPinDialog();
                break;
            case UNLOCK_PUK_REENTER_NEW_PIN_MODE:
                if (!mPin.equals(mNewPin)) {
                    mError = mRes.getString(R.string.sim_pins_dont_match);
                    mDialogState = UNLOCK_PUK_ENTER_NEW_PIN_MODE;
                    mPin = null;
                    showPinDialog();
                } else {
                    mError = null;
                    unlockPuk();
                }
                break;
            default:
                break;
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPinToggle) {
            // Get the new, preferred state
            mToState = mPinToggle.isChecked();
            // Flip it back and pop up pin dialog
            mPinToggle.setChecked(!mToState);
            mDialogState = ICC_LOCK_MODE;
            showPinDialog();
        } else if (preference == mPinDialog) {
            setDefaultPinMode();
            //mDialogState = ICC_OLD_MODE;
            return false;
        }
        return true;
    }

    private void tryChangeIccLockState() {
        // Try to change icc lock. If it succeeds, toggle the lock state and
        // reset dialog state. Else inject error message and show dialog again.
        Message callback = Message.obtain(mHandler, MSG_ENABLE_ICC_PIN_COMPLETE);
        if(DBG) Log.d(TAG,"tryChangeIccLockState(), mToState:" + mToState);
        mPhone.getIccCard().setIccLockEnabled(mToState, mPin, callback);
        // Disable the setting till the response is received.
        mPinToggle.setEnabled(false);
    }

    private void iccLockChanged(boolean success, int attemptsRemaining) {
        if (success) {
            if(DBG) {
                Log.d(TAG,"iccLockChanged(), mToState:" + mToState);
                Log.d(TAG,"iccLockChanged(), mPhone.getIccCard().getIccLockEnabled():" + mPhone.getIccCard().getIccLockEnabled());
            }
            mPinToggle.setChecked(mToState);

            /* After user change the "Lock SIM card" checkbox status and it was changed successfully.
                     * We should Enable/disable the "Change SIM PIN" EditPinPreference depend on the lock SIM change result. */
            if(mPhone.getIccCard().getIccLockEnabled())
                mPinDialog.setEnabled(true);
            else
                mPinDialog.setEnabled(false);

        } else {
            Toast.makeText(this, getPinPasswordErrorMessage(attemptsRemaining), Toast.LENGTH_LONG)
                    .show();
        }
        mPinToggle.setEnabled(true);
        resetDialogState();
    }

    private void iccPinChanged(boolean success, int attemptsRemaining) {
        if (!success) {
            Toast.makeText(this, getPinPasswordErrorMessage(attemptsRemaining),
                    Toast.LENGTH_LONG)
                    .show();
        } else {
            Toast.makeText(this, mRes.getString(R.string.sim_change_succeeded),
                    Toast.LENGTH_SHORT)
                    .show();

        }
        resetDialogState();
    }

    private void tryChangePin() {
        Message callback = Message.obtain(mHandler, MSG_CHANGE_ICC_PIN_COMPLETE);
        mPhone.getIccCard().changeIccLockPassword(mOldPin,
                mNewPin, callback);
    }

    private String getPinPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;

        if (attemptsRemaining == 0) {
            displayMessage = mRes.getString(R.string.wrong_pin_code_pukked);
        } else if (attemptsRemaining > 0) {
            displayMessage = mRes
                    .getQuantityString(R.plurals.wrong_pin_code, attemptsRemaining,
                            attemptsRemaining);
        } else {
            displayMessage = mRes.getString(R.string.pin_failed);
        }
        if (DBG) Log.d(TAG, "getPinPasswordErrorMessage:"
                + " attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    private boolean reasonablePin(String pin) {
        if (pin == null || pin.length() < MIN_PIN_LENGTH || pin.length() > MAX_PIN_LENGTH) {
            return false;
        } else {
            return true;
        }
    }

    private void resetDialogState() {
        mError = null;
        setDefaultPinMode();
        //mDialogState = ICC_OLD_MODE; // Default for when Change PIN is clicked
        mPin = "";
        setDialogValues();
        mDialogState = OFF_MODE;
    }


    /*------------------------Added for unlock pin, some code copied from SimUnlockScreen.java -------------------------------*/

    private ProgressDialog mSimUnlockProgressDialog = null;

    private void tryUnlockSimIfNeededAndChangeIccLockState(){
        //If we just need to enable icc lock state, then do not checkPin() at all.
        if (mToState){
            tryChangeIccLockState();
            //android.util.Log.d("IccLockSettings", "====> just go to change lock state to true, so do all as usual");
            return;
        }

        boolean hasIccCard = mPhone.getIccCard().hasIccCard();
        IccCardConstants.State state = mPhone.getIccCard().getState();
        //We have to unlock sim first, then we can change icc lock state
        if (hasIccCard && state == IccCardConstants.State.PIN_REQUIRED){
            //android.util.Log.d("IccLockSettings", "====> We have the icc card existed, and pin required, so checkPin() first");
            checkPin();
        } else {
            //android.util.Log.d("IccLockSettings", "====> We do not have the icc card existed,or pin is not required, do as usual");
            tryChangeIccLockState();
        }
    }

    /**
     * Since the IPC can block, we want to run the request in a separate thread
     * with a callback.
     */
    private abstract class CheckSimPin extends Thread {

        private final String mPin;

        protected CheckSimPin(String pin) {
            mPin = pin;
        }

        abstract void onSimLockChangedResponse(boolean success);

        @Override
        public void run() {
            try {
                //<for dual mode>
                ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService(SimCardID.ID_ONE.toInt() == mSimId?Context.TELEPHONY_SERVICE2:Context.TELEPHONY_SERVICE1));
                final boolean result = phone.supplyPin(mPin);
                mHandler.post(new Runnable() {
                    public void run() {
                        onSimLockChangedResponse(result);
                    }
                });
            } catch (RemoteException e) {
                mHandler.post(new Runnable() {
                    public void run() {
                        onSimLockChangedResponse(false);
                    }
                });
            }
        }
    }

    private Dialog getSimUnlockProgressDialog() {
        if (mSimUnlockProgressDialog == null) {
            mSimUnlockProgressDialog = new ProgressDialog(this);
            mSimUnlockProgressDialog.setMessage(
                    getString(R.string.lockscreen_sim_unlock_progress_dialog_message));
            mSimUnlockProgressDialog.setIndeterminate(true);
            mSimUnlockProgressDialog.setCancelable(false);
        }
        return mSimUnlockProgressDialog;
    }

    private void checkPin() {
        getSimUnlockProgressDialog().show();

        new CheckSimPin(mPin) {
            void onSimLockChangedResponse(boolean success) {
        //Move the code to iccLockChanged(), because it seems take a little longer to change the state, if just have the sim unlocked.
        //if (mSimUnlockProgressDialog != null) {
                //    mSimUnlockProgressDialog.hide();
                //}
                if (success) {
            //android.util.Log.d("IccLockSettings", "====> We have icc card unlocked just now, so try change icc lock state");
            //We have unlocked sim, so try to change icc lock state
                    tryChangeIccLockState();
                } else {
            //Unable to unlock sim, just indicate we failed to change icc lock state
            iccLockChanged(false);
            //android.util.Log.d("IccLockSettings", "====> We failed to have icc card unlocked just now, just indicate failed");
                }
            }
        }.start();
    }

    private void unlockPin() {
        getSimUnlockProgressDialog().show();
        new CheckSimPin(mPin) {
            void onSimLockChangedResponse(boolean success) {
                if (mSimUnlockProgressDialog != null && mSimUnlockProgressDialog.isShowing()) {
                    mSimUnlockProgressDialog.dismiss();
                }
                if (success) {
                    Toast.makeText(IccLockSettings.this, mRes.getString(R.string.sim_unlock_succeeded), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(IccLockSettings.this, mRes.getString(R.string.sim_lock_failed), Toast.LENGTH_SHORT).show();
                }
                resetDialogState();
            }
        }.start();
    }

    /**
     * Since the IPC can block, we want to run the request in a separate thread
     * with a callback.
     */
    private abstract class CheckSimPuk extends Thread {

        private final String mPukCode;
        private final String mNewPinCode;

        protected CheckSimPuk(String puk, String newPin) {
            mPukCode = puk;
            mNewPinCode = newPin;
        }

        abstract void onSimPukChangedResponse(boolean success);
        @Override
        public void run() {
            try {
                ITelephony phone;
                phone = ITelephony.Stub.asInterface(ServiceManager.checkService(SimCardID.ID_ONE.toInt() == mSimId?Context.TELEPHONY_SERVICE2:Context.TELEPHONY_SERVICE1));

                final boolean result = phone.supplyPuk(mPukCode, mNewPinCode);
                mHandler.post(new Runnable() {
                    public void run() {
                        onSimPukChangedResponse(result);
                    }
                });
            } catch (RemoteException e) {
                mHandler.post(new Runnable() {
                    public void run() {
                        onSimPukChangedResponse(false);
                    }
                });
            }
        }
    }

    private void unlockPuk() {
        getSimUnlockProgressDialog().show();

        new CheckSimPuk(mPuk, mNewPin) {
            void onSimPukChangedResponse(boolean success) {
                if (mSimUnlockProgressDialog != null && mSimUnlockProgressDialog.isShowing()) {
                    mSimUnlockProgressDialog.dismiss();
                }
                if (success) {
                    Toast.makeText(IccLockSettings.this, mRes.getString(R.string.sim_unlock_succeeded), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(IccLockSettings.this, mRes.getString(R.string.sim_puk_unlock_failed), Toast.LENGTH_SHORT).show();
                }
                resetDialogState();
            }
        }.start();
    }

    private void setDefaultPinMode() {
        mDialogState = ICC_OLD_MODE;
        if (mPhone.getIccCard().hasIccCard()) {
            IccCardConstants.State state = mPhone.getIccCard().getState();
            if (IccCardConstants.State.PIN_REQUIRED == state) {
                mDialogState = UNLOCK_PIN_MODE;
            } else if (IccCardConstants.State.PUK_REQUIRED == state || 0 == getPinRemainingCount(0)) {
                mDialogState = UNLOCK_PUK_MODE;
            }
        }

        switch(mDialogState) {
            case UNLOCK_PIN_MODE:
            case UNLOCK_PUK_MODE:
            case UNLOCK_PUK_ENTER_NEW_PIN_MODE:
            case UNLOCK_PUK_REENTER_NEW_PIN_MODE:
                mPinToggle.setEnabled(false);
                break;
            default:
                mPinToggle.setEnabled(true);
                break;
        }
    }
}

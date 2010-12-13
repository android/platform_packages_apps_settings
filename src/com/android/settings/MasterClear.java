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

import com.android.internal.widget.LockPatternUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

/**
 * Confirm and execute a reset of the device to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE PHONE" prompt. Finally if battery level is below a predefined value of
 * MASTER_CLEAR_EXECUTE_LEVEL the master clear will not be allowed to proceed.
 * Instead a dialog will show and inform the user. If at any time the phone is
 * allowed to go to sleep, is locked, et cetera, then the confirmation sequence
 * is abandoned.
 */
public class MasterClear extends Activity {

    private static final int KEYGUARD_REQUEST = 55;
    /** Master clear minimum execute level. */
    private static final int MASTER_CLEAR_EXECUTE_LEVEL = 30;
    /** Denominator used to calculate Master clear execute level. */
    private static final int DENOMINATOR = 100;
    /** Dialog ID of MasterClear failed. */
    private static final int DIALOG_MASTER_CLEAR_FAILED = 0;
    /** Dialog ID of MasterClear battery short. */
    private static final int DIALOG_MASTER_CLEAR_BATTERY_SHORT = 1;

    private LayoutInflater mInflater;
    private LockPatternUtils mLockUtils;

    private View mInitialView;
    private Button mInitiateButton;

    private View mFinalView;
    private Button mFinalButton;
    /** Battery low flag */
    private boolean mBatteryLevelOk;
    /** Is the BroadcastReceiver registered */
    private boolean mReceiverRegistered;
    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                setBatteryLow(intent);
            }
        }
    };

    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and invoke the Checkin Service to reset the device to its factory-default
     * state (rebooting in the process).
     */
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {
            public void onClick(View v) {
                if (Utils.isMonkeyRunning()) {
                    return;
                }

                if (mBatteryLevelOk) {
                    sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                    // Intent handling is asynchronous -- assume it will happen soon.
                } else {
                    // Alert dialog (low battery alert).
                    showDialog(DIALOG_MASTER_CLEAR_BATTERY_SHORT);
                }
            }
        };

    /**
     * Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     * @param request the request code to be returned once confirmation finishes
     * @return true if confirmation launched
     */
    private boolean runKeyguardConfirmation(int request) {
        return new ChooseLockSettingsHelper(this)
                .launchConfirmationActivity(request,
                        getText(R.string.master_clear_gesture_prompt),
                        getText(R.string.master_clear_gesture_explanation));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != KEYGUARD_REQUEST) {
            return;
        }

        // If the user entered a valid keyguard trace, present the final
        // confirmation prompt; otherwise, go back to the initial state.
        if (resultCode == Activity.RESULT_OK) {
            establishFinalConfirmationState();
        } else if (resultCode == Activity.RESULT_CANCELED) {
            finish();
        } else {
            establishInitialState();
        }
    }

    /**
     * If the user clicks to begin the reset sequence, we next require a
     * keyguard confirmation if the user has currently enabled one.  If there
     * is no keyguard available, we simply go to the final confirmation prompt.
     */
    private Button.OnClickListener mInitiateListener = new Button.OnClickListener() {
            public void onClick(View v) {
                if (!runKeyguardConfirmation(KEYGUARD_REQUEST)) {
                    establishFinalConfirmationState();
                }
            }
        };

    /**
     * Configure the UI for the final confirmation interaction
     */
    private void establishFinalConfirmationState() {
        if (mFinalView == null) {
            mFinalView = mInflater.inflate(R.layout.master_clear_final, null);
            mFinalButton =
                    (Button) mFinalView.findViewById(R.id.execute_master_clear);
            mFinalButton.setOnClickListener(mFinalClickListener);
        }
        registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mReceiverRegistered = true;
        setContentView(mFinalView);
    }

    /**
     * In its initial state, the activity presents a button for the user to
     * click in order to initiate a confirmation sequence.  This method is
     * called from various other points in the code to reset the activity to
     * this base state.
     *
     * <p>Reinflating views from resources is expensive and prevents us from
     * caching widget pointers, so we use a single-inflate pattern:  we lazy-
     * inflate each view, caching all of the widget pointers we'll need at the
     * time, then simply reuse the inflated views directly whenever we need
     * to change contents.
     */
    private void establishInitialState() {
        if (mInitialView == null) {
            mInitialView = mInflater.inflate(R.layout.master_clear_primary, null);
            mInitiateButton =
                    (Button) mInitialView.findViewById(R.id.initiate_master_clear);
            mInitiateButton.setOnClickListener(mInitiateListener);
        }
        if (mReceiverRegistered) {
            unregisterReceiver(mBatteryReceiver);
            mReceiverRegistered = false;
        }
        setContentView(mInitialView);
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mInitialView = null;
        mFinalView = null;
        mInflater = LayoutInflater.from(this);
        mLockUtils = new LockPatternUtils(this);

        // Setup initial values
        mBatteryLevelOk = true;
        mReceiverRegistered = false;

        establishInitialState();
    }

    /** Abandon all progress through the confirmation sequence by returning
     * to the initial view any time the activity is interrupted (e.g. by
     * idle timeout).
     */
    @Override
    public void onPause() {
        super.onPause();

        establishInitialState();
    }

    private void setBatteryLow(final Intent intent) {
        int level = intent.getIntExtra("level", -1);
        int scale = intent.getIntExtra("scale", -1);
        mBatteryLevelOk = scale > 0 &&
            level * DENOMINATOR / scale >= MASTER_CLEAR_EXECUTE_LEVEL;
    }

    @Override
    protected Dialog onCreateDialog(int dialogId) {
        Dialog dialog = null;
        switch (dialogId) {
        case DIALOG_MASTER_CLEAR_FAILED:
            dialog = new AlertDialog.Builder(MasterClear.this)
                    .setMessage(getText(R.string.master_clear_failed))
                    .setPositiveButton(getText(android.R.string.ok), null)
                    .create();
            break;
        case DIALOG_MASTER_CLEAR_BATTERY_SHORT:
            dialog = new AlertDialog.Builder(MasterClear.this)
                    .setTitle(R.string.master_clear_dialog_title_battery_short_txt)
                    .setMessage(getText(R.string.master_clear_dialog_message_battery_short_txt))
                    .setPositiveButton(getText(android.R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog,
                                    final int button) {
                                    establishInitialState();
                                }
                            }
                    )
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        public boolean onKey(final DialogInterface d,
                                final int keyCode, final KeyEvent event) {
                            if (KeyEvent.ACTION_DOWN == event.getAction()) {
                                switch (keyCode) {
                                    case KeyEvent.KEYCODE_BACK:
                                        establishInitialState();
                                        d.dismiss();
                                        return true;
                                    default:
                                        break;
                                }
                            }
                            return false;
                        }
                    }).create();
            break;
        default:
            break;
        }
        return dialog;
    }

    @Override
    public void onBackPressed() {
        if (mInitialView.isShown()) {
            super.onBackPressed();
        } else {
            establishInitialState();
        }
    }

}

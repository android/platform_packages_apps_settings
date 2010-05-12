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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ICheckinService;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

/**
 * Confirm and execute a reset of the device to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE PHONE" prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 */
public class MasterClear extends Activity {

    private static final int KEYGUARD_REQUEST = 55;

    private LayoutInflater mInflater;
    private LockPatternUtils mLockUtils;

    private View mInitialView;
    private Button mInitiateButton;

    private View mFinalView;
    private Button mFinalButton;

    private AlertDialog mDialog = null;

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

                ICheckinService service = 
                        ICheckinService.Stub.asInterface(ServiceManager.getService("checkin"));
                if (service != null) {
                    try {
                        // This RPC should never return
                        service.masterClear();
                    } catch (android.os.RemoteException e) {
                        // Intentionally blank - there's nothing we can do here
                        Log.w("MasterClear", "Unable to invoke ICheckinService.masterClear()");
                    }
                } else {
                    Log.w("MasterClear", "Unable to locate ICheckinService");
                }

                /* If we reach this point, the master clear didn't happen -- the
                 * service might have been unregistered with the ServiceManager,
                 * the RPC might have thrown an exception, or for some reason
                 * the implementation of masterClear() may have returned instead
                 * of resetting the device.
                 */
                if (mDialog != null) {
                    mDialog.show();
                } else {
                    mDialog = new AlertDialog.Builder(MasterClear.this)
                            .setMessage(getText(R.string.master_clear_failed))
                            .setPositiveButton(getText(android.R.string.ok), null)
                            .show();
                }
            }
        };

    /**
     *  Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     */
    private void runKeyguardConfirmation() {
        final Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                "com.android.settings.ConfirmLockPattern");
        // supply header and footer text in the intent
        intent.putExtra(ConfirmLockPattern.HEADER_TEXT,
                getText(R.string.master_clear_gesture_prompt));
        intent.putExtra(ConfirmLockPattern.FOOTER_TEXT,
                getText(R.string.master_clear_gesture_explanation));
        startActivityForResult(intent, KEYGUARD_REQUEST);
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
                if (mLockUtils.isLockPatternEnabled()) {
                    runKeyguardConfirmation();
                } else {
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

        setContentView(mInitialView);
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mInitialView = null;
        mFinalView = null;
        mInflater = LayoutInflater.from(this);
        mLockUtils = new LockPatternUtils(getContentResolver());

        establishInitialState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
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

}

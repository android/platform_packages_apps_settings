/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.development;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.development.tests.WirelessDebuggingManager;
import com.android.settings.development.tests.WirelessDebuggingManager.PairedDevice;
import com.android.settings.R;

public class AdbWirelessDialog extends AlertDialog implements
        AdbWirelessDialogUiBase,
        DialogInterface.OnClickListener {

    public interface AdbWirelessDialogListener {
        default void onCancel(PairedDevice pairDevice) {
        }
        default void onSubmit(AdbWirelessDialog dialog) {
        }
        default void onDismiss(PairedDevice pairDevice, Integer result) {
        }
    }

    private static final String TAG = "AdbWirelessDialog";

    private static final int BUTTON_CANCEL = DialogInterface.BUTTON_NEGATIVE;
    private static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;

    private final AdbWirelessDialogListener mListener;
    private final PairedDevice mPairingDevice;
    private final int mMode;
    // Indicator of whether the user has succuessfully paired with the device yet.
    private boolean mIsPaired;

    private View mView;
    private AdbWirelessDialogController mController;

    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WirelessDebuggingManager.WIRELESS_DEBUG_PAIR_STATUS_ACTION.equals(action)) {
                Integer res = intent.getIntExtra(
                        WirelessDebuggingManager.PAIR_STATUS_EXTRA,
                        WirelessDebuggingManager.RESULT_FAILED);
                PairedDevice pd = (PairedDevice) intent.getSerializableExtra(
                        WirelessDebuggingManager.PAIRED_DEVICE_EXTRA);
                if (!pd.getDeviceId().equals(mPairingDevice.getDeviceId())) {
                    // Ignore messages about other devices.
                    return;
                }

                if (res.equals(WirelessDebuggingManager.RESULT_AUTH_CODE)) {
                    mController.setPairingCode(
                            intent.getStringExtra(
                                WirelessDebuggingManager.AUTH_CODE_EXTRA));
                } else if (res.equals(WirelessDebuggingManager.RESULT_OK)) {
                    mIsPaired = true;
                    dismiss(res);
                } else if (res.equals(WirelessDebuggingManager.RESULT_FAILED)) {
                    dismiss(res);
                }
            }
        }
    };

    /**
     * Creates a AdbWirelessDialog with no additional style. It displays as a dialog above the current
     * view.
     */
    public static AdbWirelessDialog createModal(
            Context context,
            AdbWirelessDialogListener listener,
            PairedDevice pairingDevice,
            int mode) {
        return new AdbWirelessDialog(context, listener, pairingDevice, mode);
    }

    /* package */ AdbWirelessDialog(Context context, AdbWirelessDialogListener listener, PairedDevice pairingDevice, int mode) {
        super(context);
        mListener = listener;
        mPairingDevice = pairingDevice;
        mMode = mode;
        mIsPaired = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.adb_wireless_dialog, null);
        setView(mView);
        mController = new AdbWirelessDialogController(this, mView, mPairingDevice, mMode);
        mIntentFilter = new IntentFilter(WirelessDebuggingManager.WIRELESS_DEBUG_PAIR_STATUS_ACTION);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        getContext().registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();

        getContext().unregisterReceiver(mReceiver);
    }

    public Bundle onSaveInstanceState() {
        if (mMode == AdbWirelessDialogUiBase.MODE_PAIRING) {
            if (!mIsPaired && mListener != null) {
                // Cancel the pairing if user exits the dialog.
                mListener.onCancel(mPairingDevice);
            }
        }

        return super.onSaveInstanceState();
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int id) {
        if (mListener != null) {
            switch (id) {
                case BUTTON_CANCEL:
                    mListener.onCancel(mPairingDevice);
                    break;
            }
        }
    }

    public void dismiss(Integer result) {
        dismiss();
        if (mListener != null) {
            mListener.onDismiss(mPairingDevice, result);
        }
    }

    @Override
    public AdbWirelessDialogController getController() {
        return mController;
    }

    @Override
    public void dispatchSubmit() {
        if (mListener != null) {
            mListener.onSubmit(this);
        }
        dismiss();
    }

    @Override
    public int getMode() {
        return mMode;
    }

    @Override
    public Button getSubmitButton() {
        return getButton(BUTTON_SUBMIT);
    }

    @Override
    public Button getCancelButton() {
        return getButton(BUTTON_NEGATIVE);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        setButton(BUTTON_SUBMIT, text, this);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        setButton(BUTTON_NEGATIVE, text, this);
    }
}

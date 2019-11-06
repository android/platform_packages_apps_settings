/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.settings.network.telephony;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.android.settings.R;

/**
 * A dialog fragment that asks the user to disable mobile data to scan network.
 */
public class NetworkScanWarningDialogFragment extends DialogFragment implements OnClickListener {

    public static final String TAG = "NetworkScanWarningDialogFragment";

    /**
     * The interface we expect a listener to implement.
     */
    public interface NetworkScanWarningDialogListener {
        void onPositiveButtonClick(DialogFragment dialog);
        void onNegativeButtonClick(DialogFragment dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getResources().getString(R.string.network_scan_warning_message))
                .setTitle(R.string.network_scan_warning_title)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        NetworkScanWarningDialogListener listener = getListener();
        if (listener != null) {
            switch(which) {
                case DialogInterface.BUTTON_POSITIVE:
                    listener.onPositiveButtonClick(this);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                default:
                    listener.onNegativeButtonClick(this);
                    break;
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        NetworkScanWarningDialogListener listener = getListener();
        if (listener != null) {
            listener.onNegativeButtonClick(this);
        }
    }

    private NetworkScanWarningDialogListener getListener() {
        return (NetworkScanWarningDialogListener) getParentFragment();
    }
}

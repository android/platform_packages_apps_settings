/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.os.Bundle;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsRcsManager;
import android.telephony.ims.RcsUceAdapter;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class ContactDiscoveryDialogFragment extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    private static final String TAG = "ContactDiscoveryDialog";
    private static final String SUB_ID_KEY = "sub_id_key";

    private int mSubId;

    public static ContactDiscoveryDialogFragment newInstance(int subId) {
        final ContactDiscoveryDialogFragment dialogFragment = new ContactDiscoveryDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(SUB_ID_KEY, subId);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Bundle args = getArguments();
        mSubId = args.getInt(SUB_ID_KEY);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final int title = R.string.contact_discovery_opt_in_dialog_title;
        int message = R.string.contact_discovery_opt_in_dialog_message;
        builder.setMessage(getResources().getString(message))
                .setTitle(title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // let the host know that the positive button has been clicked
        if (which == dialog.BUTTON_POSITIVE) {
            setDiscoveryEnabled(true /*isEnabled*/);
        }
    }

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_UNKNOWN;
    }

    private void setDiscoveryEnabled(boolean isEnabled) {
        ImsRcsManager manager = getImsRcsManager();
        if (manager == null) return;
        RcsUceAdapter adapter = manager.getUceAdapter();
        try {
            adapter.setUceSettingEnabled(isEnabled);
        } catch (ImsException e) {
            Log.w(TAG, "UCE service is not available: " + e.getMessage());
        }
    }

    private ImsRcsManager getImsRcsManager() {
        ImsManager manager = getContext().getSystemService(ImsManager.class);
        if (manager == null) return null;
        try {
            return manager.getImsRcsManager(mSubId);
        } catch (Exception e) {
            Log.w(TAG, "Could not resolve ImsMmTelManager: " + e.getMessage());
        }
        return null;
    }
}
